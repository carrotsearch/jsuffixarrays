package org.jsuffixarrays;

import static org.jsuffixarrays.Tools.assertAlways;

import com.carrotsearch.hppc.sorting.IndirectComparator;
import com.carrotsearch.hppc.sorting.IndirectSort;
import com.google.common.primitives.Ints;

/**
 * A naive implementation of suffix sorting based on primitive integer collections and
 * custom sorting routines (quicksort).
 */
public final class NaiveSort implements ISuffixArrayBuilder
{
    /**
     * {@inheritDoc}
     * <p>
     * Additional constraints enforced by this implementation:
     * <ul>
     * <li>{@link Integer#MIN_VALUE} must not occur in the input</li>,
     * <li><code>input.length</code> &gt;= <code>start + length + 1</code> (to simplify
     * border cases)</li>
     * </ul>
     */
    @Override
    public int [] buildSuffixArray(int [] input, int start, int length)
    {
        assertAlways(input != null, "input must not be null");
        assertAlways(input.length >= start + length + 1, "no extra space after input end");

        assert Ints.asList(input).subList(start, start + length).indexOf(
            Integer.MIN_VALUE) < 0 : "Integer.MIN_VALUE must not occur in the input";

        final int saveEOS = input[start + length];
        try
        {
            input[start + length] = Integer.MIN_VALUE;
            final int [] sa = IndirectSort.sort(start, length,
                new SuffixComparator(input));
            for (int i = 0; i < sa.length; i++)
            {
                sa[i] -= start;
            }
            return sa;
        }
        finally
        {
            input[start + length] = saveEOS;
        }
    }

    /**
     * Calculates the longest common prefix (LCP) values using naive comparison of
     * adjecent entries in the suffix array.
     */
    public static int [] computeLCP(int [] input, int start, int length,
        int [] suffixArray)
    {
        assert length == suffixArray.length;

        final int [] lcpArray = new int [length];
        lcpArray[0] = -1;
        for (int i = 1; i < length; i++)
        {
            int lcp = 0;
            while (input[suffixArray[i - 1] + lcp] == input[suffixArray[i] + lcp])
            {
                lcp++;
            }
            lcpArray[i] = lcp;
        }

        return lcpArray;
    }
}

/**
 * An indirect suffix comparator. We assume the input sequence is terminated with a unique
 * symbol (no range checking).
 */
final class SuffixComparator implements IndirectComparator
{
    private final int [] sequence;

    public SuffixComparator(int [] sequence)
    {
        this.sequence = sequence;
    }

    public int compare(int suffixA, int suffixB)
    {
        if (suffixA == suffixB) return 0;

        while (sequence[suffixA] == sequence[suffixB])
        {
            suffixA++;
            suffixB++;
        }

        if (sequence[suffixA] < sequence[suffixB]) return -1;
        if (sequence[suffixA] > sequence[suffixB]) return 1;

        /* Theoretically unreachable. */
        throw new RuntimeException("Unreachable state.");
    }
}