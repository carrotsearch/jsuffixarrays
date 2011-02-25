package org.jsuffixarrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link Tools}.
 */
public class ToolsTest
{
    @Test
    public void testAllPositive()
    {
        int [] input = new int []
        {
            0, -1, 0, 1, 2, 3, -1
        };
        assertTrue(Tools.allPositive(input, 3, 3));
        assertFalse(Tools.allPositive(input, 3, 4));
        assertFalse(Tools.allPositive(input, 2, 1));
        assertFalse(Tools.allPositive(input, 0, 1));
    }

    @Test
    public void testMax()
    {
        int [] input = new int []
        {
            0, -1, 0, 1, 2, 3, -1
        };
        assertEquals(0, Tools.max(input, 0, 1));
        assertEquals(0, Tools.max(input, 0, 3));
        assertEquals(1, Tools.max(input, 0, 4));
        assertEquals(3, Tools.max(input, 2, 4));
    }
}
