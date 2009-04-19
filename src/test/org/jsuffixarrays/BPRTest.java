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

    @Override
    @Ignore
    public void sameResultWithArraySlice()
    {
        // Ignore this test, DivSufSort require start == 0
    }

}
