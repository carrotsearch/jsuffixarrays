package org.jsuffixarrays;

import org.junit.Before;

/**
 * Tests for {@link QSufSort}.
 */
public class QSufSortTest extends SuffixArrayBuilderTestBase
{
    @Before
    public void setupForConstraints()
    {
        smallAlphabet = new MinMax(1, 10);
        largeAlphabet = new MinMax(1, 1000);
    }

    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance()
    {
        return new QSufSort(true);
    }

}
