param(
    [string]$PairIpPort = "",
    [string]$PairCode = "",
    [string]$ConnectIpPort = ""
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    $candidates = @(
        "E:\Android\Sdk\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "adb.exe not found. Install Android Platform-Tools or set PATH."
}

$adb = Find-Adb
Write-Host "Using ADB: $adb" -ForegroundColor Cyan

if ($PairIpPort -and $PairCode) {
    Write-Host "Pairing over Wi-Fi Wireless Debugging ($PairIpPort)..." -ForegroundColor Yellow
    & $adb pair $PairIpPort $PairCode
}
if ($ConnectIpPort) {
    Write-Host "Connecting over Wi-Fi ($ConnectIpPort)..." -ForegroundColor Yellow
    & $adb connect $ConnectIpPort
}

$devicesOut = & $adb devices
$connected = $devicesOut | Select-String -Pattern "^\S+\s+device$"
if (-not $connected) {
    Write-Host "`nNo connected Android phone detected yet." -ForegroundColor Red
    Write-Host "Connect via USB Debugging OR Wi-Fi Wireless Debugging:" -ForegroundColor Yellow
    Write-Host "  .\recover_old_playlist.ps1 -PairIpPort 192.168.1.X:PORT1 -PairCode 123456 -ConnectIpPort 192.168.1.X:PORT2"
    exit 1
}

$outDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$dbOut = Join-Path $outDir "recovered_PoojaDB.db"
$jsonOut = Join-Path $outDir "recovered_playlist.json"

# --- TIER 1: Direct SQLite pull via run-as (if 2023 APK was debuggable) ---
Write-Host "`n[Tier 1] Attempting direct SQLite database pull from com.example.poojatracker..." -ForegroundColor Cyan
cmd /c "`"$adb`" exec-out run-as com.example.poojatracker cat databases/PoojaDB > `"$dbOut`" 2>nul"
if ((Test-Path $dbOut) -and ((Get-Item $dbOut).Length -gt 100)) {
    $bytes = [System.IO.File]::ReadAllBytes($dbOut)
    $magic = [System.Text.Encoding]::ASCII.GetString($bytes, 0, [Math]::Min(15, $bytes.Length))
    if ($magic -eq "SQLite format 3") {
        Write-Host "[Tier 1 SUCCESS] Extracted raw PoojaDB ($($bytes.Length) bytes) -> $dbOut" -ForegroundColor Green
        & $adb push $dbOut /sdcard/Download/PoojaDB_recovered.db | Out-Null
        Write-Host "Pushed to phone: Downloads/PoojaDB_recovered.db" -ForegroundColor Green
        & $adb shell am start -n com.bhavya.poojatracker/com.example.poojatracker.MainActivity | Out-Null
        Write-Host "Opened Pooja Tracker -> Tap 'Import File' and select Downloads/PoojaDB_recovered.db!" -ForegroundColor Green
        exit 0
    }
}
if (Test-Path $dbOut) { Remove-Item $dbOut -Force }

# --- TIER 2: Check logcat for Log.d("export", jsonString) ---
Write-Host "[Tier 2] Checking logcat buffer for legacy export JSON..." -ForegroundColor Cyan
$logExport = & $adb logcat -d -s export:D
$joinedLog = ($logExport -join "`n")
if ($joinedLog -match '(\[\s*\{[\s\S]*\}\s*\])') {
    $capturedJson = $Matches[1]
    [System.IO.File]::WriteAllText($jsonOut, $capturedJson, [System.Text.UTF8Encoding]::new($false))
    & $adb push $jsonOut /sdcard/Download/recovered_playlist.json | Out-Null
    Write-Host "[Tier 2 SUCCESS] Recovered JSON from logcat -> $jsonOut & Downloads/recovered_playlist.json" -ForegroundColor Green
    & $adb shell am start -n com.bhavya.poojatracker/com.example.poojatracker.MainActivity | Out-Null
    exit 0
}

# --- TIER 3: Guaranteed 7-Day UI XML Scraper (works on non-debuggable APK even when Export crashes) ---
Write-Host "[Tier 3] Running automated 7-day UI scraper on com.example.poojatracker..." -ForegroundColor Cyan

function Dump-UiXml {
    & $adb shell uiautomator dump /sdcard/window_dump.xml | Out-Null
    $raw = & $adb exec-out cat /sdcard/window_dump.xml
    return [xml]($raw -join "`n")
}

function Get-BoundsCenter([string]$bounds) {
    if ($bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        $cx = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
        $cy = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
        return @($cx, $cy, [int]$Matches[1], [int]$Matches[2], [int]$Matches[3], [int]$Matches[4])
    }
    return $null
}

$daysOrder = @("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
$dayButtonIds = @(
    "com.example.poojatracker:id/monday",
    "com.example.poojatracker:id/tuesday",
    "com.example.poojatracker:id/wednesday",
    "com.example.poojatracker:id/thurday",
    "com.example.poojatracker:id/friday",
    "com.example.poojatracker:id/saturday",
    "com.example.poojatracker:id/sunday"
)

$recordsByKey = [ordered]@{}
$nextId = 100001

for ($d = 0; $d -lt 7; $d++) {
    $dayName = $daysOrder[$d]
    $btnResId = $dayButtonIds[$d]

    & $adb shell am start -W -S com.example.poojatracker/.MainActivity | Out-Null
    Start-Sleep -Milliseconds 700
    $mainXml = Dump-UiXml
    $btnNode = $mainXml.SelectSingleNode("//*[@resource-id='$btnResId']")
    if (-not $btnNode -and $d -eq 3) {
        $btnNode = $mainXml.SelectSingleNode("//*[@resource-id='com.example.poojatracker:id/thursday']")
    }
    if (-not $btnNode) {
        Write-Warning "Could not locate button for $dayName ($btnResId)"
        continue
    }

    $pt = Get-BoundsCenter $btnNode.bounds
    & $adb shell input tap $pt[0] $pt[1]
    Start-Sleep -Milliseconds 900

    $seenForDay = @{}
    $stagnant = 0
    for ($scroll = 0; $scroll -lt 25; $scroll++) {
        $listXml = Dump-UiXml
        $cards = $listXml.SelectNodes("//*[@resource-id='com.example.poojatracker:id/mainCard']")
        $addedThisPass = 0
        foreach ($card in $cards) {
            $nameNode = $card.SelectSingleNode(".//*[@resource-id='com.example.poojatracker:id/pooja_name']")
            $descNode = $card.SelectSingleNode(".//*[@resource-id='com.example.poojatracker:id/pooja_desc']")
            $urlNode  = $card.SelectSingleNode(".//*[@resource-id='com.example.poojatracker:id/contentURL']")
            $chkNode  = $card.SelectSingleNode(".//*[@resource-id='com.example.poojatracker:id/status']")

            $title = if ($nameNode) { "$($nameNode.text)".Trim() } else { "" }
            $desc  = if ($descNode) { "$($descNode.text)".Trim() } else { "" }
            $url   = if ($urlNode)  { "$($urlNode.text)".Trim() }  else { "" }
            $done  = if ($chkNode -and $chkNode.checked -eq "true") { $true } else { $false }

            if (-not $title -or -not $url -or $url -eq "Pooja url here") { continue }
            $key = "$($title.ToLowerInvariant())|$url"
            if (-not $seenForDay.ContainsKey($key)) {
                $seenForDay[$key] = $true
                $addedThisPass++
            }
            if (-not $recordsByKey.Contains($key)) {
                $recordsByKey[$key] = [ordered]@{
                    id         = $nextId++
                    title      = $title
                    desc       = $desc
                    contentURL = $url
                    status     = $done
                    days       = @(0, 0, 0, 0, 0, 0, 0)
                }
            }
            $recordsByKey[$key].days[$d] = 1
        }

        $detailsNode = $listXml.SelectSingleNode("//*[@resource-id='com.example.poojatracker:id/details']")
        $expectedTotal = -1
        if ($detailsNode -and "$($detailsNode.text)" -match '/\s*(\d+)') {
            $expectedTotal = [int]$Matches[1]
        }
        if ($expectedTotal -eq 0 -or ($expectedTotal -gt 0 -and $seenForDay.Count -ge $expectedTotal)) {
            break
        }
        if ($addedThisPass -eq 0) { $stagnant++ } else { $stagnant = 0 }
        if ($stagnant -ge 2) { break }

        $rvNode = $listXml.SelectSingleNode("//*[@resource-id='com.example.poojatracker:id/poojalist']")
        if ($rvNode) {
            $rv = Get-BoundsCenter $rvNode.bounds
            $bottomY = $rv[5] - 80
            $topY = $rv[3] + 80
            & $adb shell input swipe $rv[0] $bottomY $rv[0] $topY 350
            Start-Sleep -Milliseconds 650
        } else {
            break
        }
    }
    Write-Host "  $dayName : captured $($seenForDay.Count) Poojas" -ForegroundColor Gray
}

$exportList = @($recordsByKey.Values)
$jsonPayload = ConvertTo-Json -InputObject $exportList -Depth 5
if ($exportList.Count -eq 1) { $jsonPayload = "[$jsonPayload]" }
if ($exportList.Count -eq 0) { $jsonPayload = "[]" }

[System.IO.File]::WriteAllText($jsonOut, $jsonPayload, [System.Text.UTF8Encoding]::new($false))
& $adb push $jsonOut /sdcard/Download/recovered_playlist.json | Out-Null

Write-Host "`n[Tier 3 SUCCESS] Recovered $($exportList.Count) unique Poojas!" -ForegroundColor Green
Write-Host "  Saved on PC   : $jsonOut" -ForegroundColor Green
Write-Host "  Saved on Phone: Downloads/recovered_playlist.json" -ForegroundColor Green
& $adb shell am start -n com.bhavya.poojatracker/com.example.poojatracker.MainActivity | Out-Null
Write-Host "Opened Pooja Tracker -> Tap 'Import File' and select Downloads/recovered_playlist.json!" -ForegroundColor Yellow
