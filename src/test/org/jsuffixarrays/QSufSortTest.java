package org.jsuffixarrays;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests for {@link QSufSort}.
 */
public class QSufSortTest extends SuffixArrayBuilderTestBase {
    @Before
    public void setupForConstraints() {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, 1000);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance() {
        return new QSufSort(true);
    }

    /*
     * 
     */
    @Override
    @Ignore
    public void sameResultWithArraySlice() {
        // Ignore this test, LS require start == 0
    }

}
