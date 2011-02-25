package org.jsuffixarrays;

import org.junit.Before;

/**
 * Tests for {@link DivSufSort}.
 */
public class DivSufSortTest extends SuffixArrayBuilderTestBase
{
    private final int alphabetSize = 256;

    @Before
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
