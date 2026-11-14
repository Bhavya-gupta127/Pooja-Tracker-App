# One-Click USB Playlist Migration: Old App (com.example.poojatracker) -> New App (com.bhavya.poojatracker)
# Usage: Connect your Android phone via USB with USB Debugging enabled, then run:
#   powershell -ExecutionPolicy Bypass -File .\migrate_playlist_usb.ps1

$ErrorActionPreference = "Stop"
$adb = "E:\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

Write-Host "Checking connected Android phone via ADB..." -ForegroundColor Cyan
$devices = & $adb devices | Select-String -Pattern "\tdevice$"
if (-not $devices) {
    Write-Host "No phone detected! Plug in your phone via USB, enable USB Debugging, accept the RSA prompt, and re-run." -ForegroundColor Yellow
    exit 1
}

$apkPath = "E:\startup\PoojaTracker-v1.2-NoConflict.apk"
if (Test-Path $apkPath) {
    Write-Host "Installing new Pooja Tracker v1.2 side-by-side (leaves old app untouched)..." -ForegroundColor Cyan
    & $adb install -r $apkPath
}

$localDb = Join-Path $PSScriptRoot "PoojaDB_backup.sqlite"
Write-Host "Extracting PoojaDB from old app (com.example.poojatracker)..." -ForegroundColor Cyan

# Method 1: Direct debuggable run-as extraction (works if old app was installed from Android Studio)
cmd /c "`"$adb`" exec-out run-as com.example.poojatracker cat databases/PoojaDB > `"$localDb`"" 2>$null

$validDb = $false
if ((Test-Path $localDb) -and ((Get-Item $localDb).Length -gt 1024)) {
    $bytes = [System.IO.File]::ReadAllBytes($localDb)
    $header = [System.Text.Encoding]::ASCII.GetString($bytes, 0, [Math]::Min(15, $bytes.Length))
    if ($header -eq "SQLite format 3") {
        $validDb = $true
    }
}

if ($validDb) {
    Write-Host "Extracted PoojaDB ($((Get-Item $localDb).Length) bytes)! Pushing to Downloads & injecting into new Pooja Tracker..." -ForegroundColor Green
    & $adb push $localDb /sdcard/Download/PoojaDB_backup.db | Out-Null
    # Launch new app once so its data directory exists, then stop and copy DB directly
    & $adb shell monkey -p com.bhavya.poojatracker -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Seconds 2
    & $adb shell am force-stop com.bhavya.poojatracker
    & $adb shell "run-as com.bhavya.poojatracker mkdir -p databases"
    cmd /c "`"$adb`" exec-out cat /sdcard/Download/PoojaDB_backup.db | `"$adb`" shell `"run-as com.bhavya.poojatracker sh -c 'cat > databases/PoojaDB'`""
    & $adb shell "run-as com.bhavya.poojatracker rm -f databases/PoojaDB-journal databases/PoojaDB-wal databases/PoojaDB-shm"
    & $adb shell monkey -p com.bhavya.poojatracker -c android.intent.category.LAUNCHER 1 | Out-Null
    Write-Host "SUCCESS! Your entire playlist is now live in the new 'Pooja Tracker' app (and backed up at Downloads/PoojaDB_backup.db)!" -ForegroundColor Green
} else {
    Write-Host "Old app is non-debuggable (release build). Checking Downloads for any exported output*.json..." -ForegroundColor Yellow
    & $adb shell "ls -lh /sdcard/Download/output*.json"
    Write-Host "Tip: Open your old 'Pooja Scheduler' app -> tap ADD -> tap EXPORT, then open new 'Pooja Tracker' -> ADD -> Import File -> pick output*.json!" -ForegroundColor Cyan
}
