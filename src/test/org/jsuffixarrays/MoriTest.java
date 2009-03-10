package org.jsuffixarrays;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests for {@link Mori}.
 */
public class MoriTest extends SuffixArrayBuilderTestBase {
    @Before
    public void setupForConstraints() {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, Mori.ALPHABET_SIZE - 1);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance() {
        return new Mori();
    }

    /*
     * 
     */
    @Override
    @Ignore
    public void sameResultWithArraySlice()
    {
        // Ignore this test, Mori require start == 0
    }
}
