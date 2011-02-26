package org.jsuffixarrays;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests for {@link SAIS}.
 */
public class SAISTest extends SuffixArrayBuilderTestBase {
    @BeforeSuite
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
    @Test(enabled = false)
    public void sameResultWithArraySlice() {
        // Ignore this test, requires start == 0
    }
}
