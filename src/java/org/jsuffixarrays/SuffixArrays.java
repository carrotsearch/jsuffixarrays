package org.jsuffixarrays;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.PrimitiveArrays;

/*
 * TODO: ultimately, this class should be "intelligent" enough to pick the best
 * algorith, depending on the distribution and properties of the input (alphabet size,
 * symbols distribution, etc.).
 */
/**
 * <p>
 * Factory-like methods for constructing suffix arrays for various data types. Whenever
 * defaults are provided, they aim to be sensible, "best guess" values for the given data
 * type.
 * <p>
 * Note the following important aspects that apply to nearly all methods in this class:
 * <ul>
 * <li>In nearly all cases, the returned suffix array will not be length-equal to the
 * input sequence (will be slightly larger). It is so because most algorithms use extra
 * space for end of sequence delimiters and it makes little sense to temporary duplicate
 * memory consumption just to have exact length counts.</li>
 * </ul>
 */
public final class SuffixArrays
{
    /**
     * Maximum required trailing space in the input array (certain algorithms need it).
     */
    final static int MAX_EXTRA_TRAILING_SPACE = DeepShallow.OVERSHOOT;

    /*
     * 
     */
    private SuffixArrays()
    {
        // no instances.
    }

    /**
     * Create a suffix array for a given character sequence with the default algorithm.
     */
    public static int [] create(CharSequence s)
    {
        return create(s, defaultAlgorithm());
    }

    /**
     * Create a suffix array for a given character sequence, using the provided suffix
     * array building strategy.
     */
    public static int [] create(CharSequence s, ISuffixArrayBuilder builder)
    {
        return new CharSequenceAdapter(builder).buildSuffixArray(s);
    }

    /**
     * Create a suffix array and an LCP array for a given character sequence.
     * 
     * @see #computeLCP(int[], int, int, int[])
     */
    public static SuffixData createWithLCP(CharSequence s)
    {
        return createWithLCP(s, defaultAlgorithm());
    }

    /**
     * Create a suffix array and an LCP array for a given character sequence, use the
     * given algorithm for building the suffix array.
     * 
     * @see #computeLCP(int[], int, int, int[])
     */
    public static SuffixData createWithLCP(CharSequence s, ISuffixArrayBuilder builder)
    {
        final CharSequenceAdapter adapter = new CharSequenceAdapter(builder);
        final int [] sa = adapter.buildSuffixArray(s);
        final int [] lcp = computeLCP(adapter.input, 0, s.length(), sa);
        return new SuffixData(sa, lcp);
    }

    /**
     * Create a suffix array and an LCP array for a given input sequence of symbols.
     */
    public static SuffixData createWithLCP(int [] input, int start, int length)
    {
        final ISuffixArrayBuilder builder = new DensePositiveDecorator(
            new ExtraTrailingCellsDecorator(defaultAlgorithm(), 3));
        return createWithLCP(input, start, length, builder);
    }

    /**
     * Create a suffix array and an LCP array for a given input sequence of symbols and a
     * custom suffix array building strategy.
     */
    public static SuffixData createWithLCP(int [] input, int start, int length,
        ISuffixArrayBuilder builder)
    {
        final int [] sa = builder.buildSuffixArray(input, start, length);
        final int [] lcp = computeLCP(input, start, length, sa);
        return new SuffixData(sa, lcp);
    }

    /**
     * Calculate longest prefix (LCP) array for an existing suffix array and input. Index
     * <code>i</code> of the returned array indicates the length of the common prefix
     * between suffix <code>i</code> and <code>i-1<code>. The 0-th
     * index has a constant value of <code>-1</code>.
     * <p>
     * The algorithm used to compute the LCP comes from
     * <tt>T. Kasai, G. Lee, H. Arimura, S. Arikawa, and K. Park. Linear-time longest-common-prefix
     * computation in suffix arrays and its applications. In Proc. 12th Symposium on Combinatorial
     * Pattern Matching (CPM ’01), pages 181–192. Springer-Verlag LNCS n. 2089, 2001.</tt>
     */
    public static int [] computeLCP(int [] input, final int start, final int length,
        int [] sa)
    {
        final int [] rank = new int [length];
        for (int i = 0; i < length; i++)
            rank[sa[i]] = i;
        int h = 0;
        final int [] lcp = new int [length];
        for (int i = 0; i < length; i++)
        {
            int k = rank[i];
            if (k == 0)
            {
                lcp[k] = -1;
            }
            else
            {
                final int j = sa[k - 1];
                while (i + h < length && j + h < length
                    && input[start + i + h] == input[start + j + h])
                {
                    h++;
                }
                lcp[k] = h;
            }
            if (h > 0) h--;
        }

        return lcp;

    }

    /**
     * 
     */
    public static void visit(final int [] sa, final int [] lcp)
    {

        // for (int i = 0; i < sa.length; i++)
        // {
        // System.out.print(sa[i] + " ");
        // }
        // System.out.println();
        //
        for (int i = 0; i < lcp.length; i++)
        {
            System.out.print(lcp[i] + " ");
        }
        System.out.println();

        foo(sa, lcp, 0, lcp.length, 0);

    }

    /**
     * 
     */
    private static void foo(final int [] sa, final int [] lcp, int start, int length,
        int x)
    {
        // System.out.println(start + " " + length + " " + x);
        StringBuilder b = new StringBuilder("");
        for (int i = 0; i < x; i++)
        {
            b.append("\t");
        }
        String pre = b.toString();

        int i = start;
        while (i < start + length)
        {
            if (lcp[i] - x <= 0) // TODO: remove this condition when algorithm is finished
            {
                // is zero is followed by another zero in lcp array, then suffix
                // identified by former one is a leaf
                if (i + 1 == start + length || lcp[i + 1] - x <= 0)
                {
                    System.out.println(pre + "leaf: sa[" + i + "]= " + sa[i]);
                }
                else
                // group of positive values (and preceding zero) represents node in suffix
                // tree
                {
                    int subtreeStart = i;
                    int subtreeLength = 1;
                    int subtreeCommonPrefixLength = Integer.MAX_VALUE;
                    while (i + 1 < start + length && lcp[i + 1] - x > 0)
                    {
                        if (lcp[i + 1] - x > 0 && lcp[i + 1] < subtreeCommonPrefixLength)
                        {
                            subtreeCommonPrefixLength = lcp[i + 1];
                        }
                        i++;
                        subtreeLength++;
                    }
                    System.out.println(pre + "node: " + subtreeStart + " -- " + (subtreeStart + subtreeLength - 1));

                    // traverse subtree
                    foo(sa, lcp, subtreeStart, subtreeLength, subtreeCommonPrefixLength);
                }
                i++;
            }
            else
            {
                throw new RuntimeException("FAIL");
            }
        }
    }

    /**
     * @return Return a new instance of the default algorithm for use in other methods.
     */
    private static ISuffixArrayBuilder defaultAlgorithm()
    {
        return new Skew();
    }

    /**
     * Utility method converting all suffixes of a given sequence to a list of strings.
     */
    public static List<CharSequence> toString(CharSequence input, int [] suffixes)
    {
        final String full = input.toString();
        final ArrayList<CharSequence> result = Lists.newArrayList();
        for (int i = 0; i < input.length(); i++)
        {
            result.add(full.subSequence(suffixes[i], full.length()));
        }
        return result;
    }

    /**
     * Utility method converting all suffixes of a given sequence of integers to a list of
     * lists of integers.
     */
    public static List<String> toString(int [] input, int start, int length,
        int [] suffixes)
    {
        final List<Integer> full = PrimitiveArrays.asList(input);
        final ArrayList<String> result = Lists.newArrayList();
        for (int i = 0; i < length; i++)
        {
            result.add(full.subList(suffixes[i], start + length).toString());
        }
        return result;
    }

    public static void main(String [] args)
    {
        SuffixData sd = createWithLCP("mississippia", new DivSufSort());
        visit(sd.getSuffixArray(), sd.getLCP());
    }

}
