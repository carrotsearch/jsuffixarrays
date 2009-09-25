package org.jsuffixarrays;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test methods not covered elsewhere.
 */
public class SuffixArraysTest
{
    /**
     * Verify that LCP array is indeed valid and correct with respect to the input.
     */
    @Test
    public void verifyLCP()
    {
        final Random rnd = new Random(0x11223344);
        final int size = 1000;
        final MinMax alphabet = new MinMax(1, 5);
        final int [] input = SuffixArrayBuilderTestBase.generateRandom(rnd, size, alphabet);

        final SuffixData data = SuffixArrays.createWithLCP(input, 0, input.length);
        final int [] lcp = data.getLCP();
        final int [] sa = data.getSuffixArray();

        Assert.assertEquals(-1, lcp[0]);
        for (int i = 1 ; i < lcp.length; i++)
        {
            Assert.assertEquals(lcp[i], prefixLength(input, sa[i], sa[i - 1]));
        }
    }

    private int prefixLength(int [] input, int i, int j)
    {
        int prefix = 0;
        while (i < input.length && j < input.length && input[i] == input[j])
        {
            prefix++;
            i++;
            j++;
        }
        return prefix;
    }
    
    /*
     * 
     */
    @Test
    public void testSuffixArrayWalk()
    {
        SuffixData sd = SuffixArrays.createWithLCP("mississippi$");
        SuffixArrays.visit(sd.getSuffixArray(), sd.getLCP(), new IVisitor()
        {
            @Override
            public boolean edge(int fromNodeStart, int fromNodeLength, int toNodeStart,
                int toNodeLength)
            {
                System.out.println("EDGE:  from " + fromNodeStart + " -- "
                    + (fromNodeStart + fromNodeLength) + " to " + toNodeStart + " -- "
                    + (toNodeStart + toNodeLength));
                return true;

            }

            @Override
            public void post(int nodeStart, int nodeLength)
            {
                System.out.println("POST: " + nodeStart + " -- "
                    + (nodeStart + nodeLength));

            }

            @Override
            public boolean pre(int nodeStart, int nodeLength)
            {
                System.out.println("PRE: " + nodeStart + " -- "
                    + (nodeStart + nodeLength));

                return true;
            }
        });
    }
}
