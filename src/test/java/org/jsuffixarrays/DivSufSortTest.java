package org.jsuffixarrays;

import org.testng.annotations.BeforeSuite;

/**
 * Tests for {@link DivSufSort}.
 */
public class DivSufSortTest extends SuffixArrayBuilderTestBase
{
    private final int alphabetSize = 256;

    @BeforeSuite
    public void setupForConstraints()
    {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, alphabetSize - 1);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance()
    {
        return new DivSufSort(alphabetSize);
    }
}
