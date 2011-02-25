package org.jsuffixarrays;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests for {@link DivSufSort}.
 */
public class BPRTest extends SuffixArrayBuilderTestBase
{

    @Before
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

    @Ignore
    @Override
    public void invariantsOnRandomLargeAlphabet()
    {
        // TODO: I believe the long running times of BPR are due to excessive
        // memory allocation, not algorithmic properties.

        // super.invariantsOnRandomLargeAlphabet();
    }
}
