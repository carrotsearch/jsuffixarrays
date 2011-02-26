package org.jsuffixarrays;

import org.testng.annotations.BeforeSuite;


/**
 * Tests for {@link DivSufSort}.
 */
public class DeepShallowTest extends SuffixArrayBuilderTestBase
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
        return new DeepShallow();
    }
}
