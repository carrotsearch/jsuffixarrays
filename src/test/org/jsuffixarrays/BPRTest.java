package org.jsuffixarrays;

import org.junit.Before;

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

}
