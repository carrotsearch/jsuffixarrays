package org.jsuffixarrays;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests for {@link DivSufSort}.
 */
public class BPRTest extends SuffixArrayBuilderTestBase
{
    @BeforeSuite
    public void setupForConstraints()
    {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, 255);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance()
    {
        return new BPR();
    }

    @Override
    @Test(enabled = false)
    public void invariantsOnRandomLargeAlphabet()
    {
        // TODO: I believe the long running times of BPR are due to excessive
        // memory allocation, not algorithmic properties.

        // super.invariantsOnRandomLargeAlphabet();
    }
}
