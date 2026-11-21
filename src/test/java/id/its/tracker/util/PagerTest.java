package id.its.tracker.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagerTest {

    @Test
    void fewPagesShowEveryLink() {
        assertEquals(List.of(1L, 2L, 3L), Pager.window(2, 3));
        assertEquals(13, Pager.window(1, 13).size());
    }

    @Test
    void manyPagesShowAWindowAroundTheCurrentPage() {
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 0L, 19L, 20L), Pager.window(7, 20));
        assertEquals(List.of(1L, 2L, 0L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 0L, 19L, 20L), Pager.window(10, 20));
        assertEquals(List.of(1L, 2L, 0L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L), Pager.window(14, 20));
    }
}
