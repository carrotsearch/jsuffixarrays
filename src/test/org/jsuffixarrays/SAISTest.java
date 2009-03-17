package org.jsuffixarrays;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests for {@link Mori}.
 */
public class SAISTest extends SuffixArrayBuilderTestBase {
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
        return new SAIS();
    }

    /*
     * 
     */
    @Override
    @Ignore
    public void sameResultWithArraySlice() {
        // Ignore this test, requires start == 0
    }
}
