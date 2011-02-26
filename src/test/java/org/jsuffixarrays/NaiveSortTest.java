package org.jsuffixarrays;

import org.testng.annotations.BeforeSuite;

/**
 * Tests for {@link NaiveSort}.
 */
public class NaiveSortTest extends SuffixArrayBuilderTestBase
{
    @BeforeSuite
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
        return new NaiveSort();
    }
}