package org.jsuffixarrays;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests for {@link DivSufSort}.
 */
public class DivSufSortTest extends SuffixArrayBuilderTestBase {
    private final int alphabetSize = 256;

    @Before
    public void setupForConstraints() {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, alphabetSize - 1);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance() {
        return new DivSufSort(alphabetSize);
    }

    /*
     * 
     */
    @Override
    @Ignore
    public void sameResultWithArraySlice() {
        // Ignore this test, DivSufSort require start == 0
    }
}
