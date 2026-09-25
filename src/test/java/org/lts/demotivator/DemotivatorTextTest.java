package org.lts.demotivator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lts.demotivator.render.DemotivatorText;

class DemotivatorTextTest {
    @Test
    void titleOnlyUsesFallbackSubtitle() {
        DemotivatorText text = DemotivatorText.parse("  Creeper  ", "footer");
        assertEquals("Creeper", text.title());
        assertEquals("footer", text.subtitle());
    }

    @Test
    void pipeSplitsTitleAndSubtitle() {
        DemotivatorText text = DemotivatorText.parse("Diamonds | they were lava all along | really", "ignored");
        assertEquals("Diamonds", text.title());
        assertEquals("they were lava all along | really", text.subtitle());
    }

    @Test
    void escapedNewlineBecomesLineBreak() {
        DemotivatorText text = DemotivatorText.parse("first \\n  second", "");
        assertEquals("first\nsecond", text.title());
    }

    @Test
    void emptyInputIsEmpty() {
        assertTrue(DemotivatorText.parse(" | ", "").isEmpty());
    }

    @Test
    void longInputIsCapped() {
        DemotivatorText text = DemotivatorText.parse("a".repeat(1000), "");
        assertEquals(DemotivatorText.MAX_LENGTH, text.title().length());
    }
}
