package com.example.poojatracker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Local unit tests covering YouTube URL parsing/validation and Pooja JSON export aggregation.
 */
public class ExampleUnitTest {
    @Test
    public void extractVideoId_handlesAllYouTubeFormats() {
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("https://www.youtube.com/watch?v=Naruxgq6oMU&list=RDNaruxgq6oMU&start_radio=1")
        );
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("https://youtu.be/Naruxgq6oMU?si=AbCdEfGhIjKlMnOp")
        );
        assertEquals(
                "4bbwANHNlEQ",
                PoojaModel.extractVideoId("https://www.youtube.com/shorts/4bbwANHNlEQ?feature=share")
        );
        assertEquals(
                "Clc7w9CeLXs",
                PoojaModel.extractVideoId("https://www.youtube.com/live/Clc7w9CeLXs?si=xyz123")
        );
        assertEquals(
                "Naruxgq6oMU",
                PoojaModel.extractVideoId("  Naruxgq6oMU  ")
        );
    }

    @Test
    public void hasValidVideoId_rejectsEmptyOrInvalidLinks() {
        assertTrue(PoojaModel.hasValidVideoId("https://www.youtube.com/watch?v=Naruxgq6oMU&list=RDNaruxgq6oMU&start_radio=1"));
        assertTrue(PoojaModel.hasValidVideoId("Naruxgq6oMU"));
        assertFalse(PoojaModel.hasValidVideoId(""));
        assertFalse(PoojaModel.hasValidVideoId("   "));
        assertFalse(PoojaModel.hasValidVideoId("not-a-youtube-link"));
        assertFalse(PoojaModel.hasValidVideoId(null));
    }

    @Test
    public void aggregatePoojaRecords_groupsDaysPerIdAndHandlesNullDaySafely() {
        List<MyDBHelper.JoinedPoojaRow> rows = Arrays.asList(
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Monday"),
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Wednesday"),
                new MyDBHelper.JoinedPoojaRow(101, "Ganesh Mantra", "108 Times", "https://www.youtube.com/watch?v=Naruxgq6oMU", true, "Sunday"),
                new MyDBHelper.JoinedPoojaRow(202, "Orphan Entry", "No days", "https://www.youtube.com/watch?v=4bbwANHNlEQ", false, null)
        );

        List<MyDBHelper.PoojaExportRecord> aggregated = MyDBHelper.aggregatePoojaRecords(rows);
        assertEquals(2, aggregated.size());

        MyDBHelper.PoojaExportRecord ganesh = aggregated.get(0);
        assertEquals(101, ganesh.id);
        assertArrayEquals(new int[]{1, 0, 1, 0, 0, 0, 1}, ganesh.days);

        MyDBHelper.PoojaExportRecord orphan = aggregated.get(1);
        assertEquals(202, orphan.id);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0}, orphan.days);
    }
}