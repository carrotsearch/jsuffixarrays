package org.jsuffixarrays;

/**
 * A holder structure for a suffix array and longest common prefix array of
 * a given sequence. 
 */
public final class SuffixData
{
    private final int [] suffixArray;
    private final int [] lcp;

    SuffixData(int [] sa, int [] lcp)
    {
        this.suffixArray = sa;
        this.lcp = lcp;
    }

    public int [] getSuffixArray()
    {
        return suffixArray;
    }

    public int [] getLCP()
    {
        return lcp;
    }
}
