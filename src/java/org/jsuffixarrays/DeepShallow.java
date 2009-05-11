package org.jsuffixarrays;

import static org.jsuffixarrays.Tools.assertAlways;

import java.util.Arrays;

//TODO: investigate strange behaviour when start != 0 in DeepShallow
/*
 * [MN]
 * how to recreate this strange behaviour:
 * 1. use System.arraycopy to create Text array (size = length + overshoot, copy input from start) __OR__ replace all occurences of Text[ with Text[start + 
 * 2. delete code from DeepShallowTest that ignores sameResultWithArraySlice()
 * 3. make sure that line Tools.assertAlways(start == 0, "start index is not zero"); in buildSuffixArray is uncommented
 * 4. run tests -- only sameResultWithArraySlice() should fail
 * 5. comment or delete Tools.assertAlways(start == 0, "start index is not zero");
 * 6. run test -- sameResultWithArraySlice() will pass, but both tests on random input will fail
 * ???
 * 
 * I tried reseting all fields of this class at the start of buildSuffixArray, but it didn't help.
 */

/**
 * <p>
 * Straightforward reimplementation of deep-shallow algorithm given in: <tt>
 * Giovanni Manzini and Paolo Ferragina. Engineering a lightweight suffix array construction algorithm.
 * </tt>
 * <p>
 * This implementation is basically a translation of the C version given by Giovanni
 * Manzini
 * <p>
 * The implementation of this algorithm makes some assumptions about the input. See
 * {@link #buildSuffixArray(int[], int, int)} for details.
 */
public class DeepShallow implements ISuffixArrayBuilder
{
    class SplitGroupResult
    {
        int equal;
        int lower;

        public SplitGroupResult(int equal, int lower)
        {
            this.equal = equal;
            this.lower = lower;
        }
    }

    class Node
    {
        int skip;
        int key;
        Node down;
        int downInt;
        Node right;
    }

    /**
     * TODO: magic constant?
     */
    public final static int overshoot = 575;

    private final static int SETMASK = 1 << 30;
    private final static int CLEARMASK = ~SETMASK;
    private final static int Mk_qs_thresh = 20;
    private final static int Max_thresh = 30;
    private final static int Shallow_limit = 550; // limit for shallow_sort
    private final static int Cmp_overshoot = 16;
    private final static int MARKER = 1 << 31;
    private final static int Max_pseudo_anchor_offset = 0;
    private final static int B2g_ratio = 1000;
    private final static int Update_anchor_ranks = 0;
    private final static int Blind_sort_ratio = 2000;
    private static final int STACK_SIZE = 100;

    private int [] Text;
    private int Text_size;
    private int [] Sa;
    private int Anchor_dist; // distance between anchors
    private int Anchor_num;
    private int [] Anchor_offset;
    private int [] Anchor_rank;
    private final int [] ftab = new int [66049];
    private final int [] bucket_ranked = new int [66049];
    private final int [] runningOrder = new int [257];
    private final int [] lcp_aux = new int [1 + Max_thresh];
    private int lcp;
    private int Cmp_left;
    private int Cmp_done;

    private int Aux;

    private int Aux_written;

    private int Stack_size;

    private Node [] Stack;

    private final boolean preserveInput;

    public DeepShallow()
    {
        preserveInput = true;
    }

    public DeepShallow(boolean preserveInput)
    {
        this.preserveInput = preserveInput;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Additional constraints enforced by Deep-Shallow algorithm:
     * <ul>
     * <li>non-negative (&ge;0) symbols in the input</li>
     * <li>maximal symbol value &lt; <code>256</code></li>
     * <li><code>input.length</code> &gt;=
     * <code>start + length + {@link #overshoot}</code></li>
     * <li>length >= 2</li>
     * <li>start == 0</li>
     * </ul>
     */
    @Override
    public int [] buildSuffixArray(int [] input, int start, int length)
    {

        Tools.assertAlways(start == 0, "start index is not zero");
        Tools.assertAlways(overshoot == Shallow_limit + Cmp_overshoot + 9, "");
        Tools.assertAlways(input.length >= start + length + overshoot, "");
        MinMax mm = Tools.minmax(input, start, length);
        assertAlways(mm.min >= 0, "input must not be negative");
        assertAlways(mm.max < 256, "max alphabet size is 256");

        lcp = 1;
        Stack = new Node [length];
        if (preserveInput)
        {
            Text = input.clone();
        }
        else
        {
            Text = input;
        }

        for (int i = length; i < length + overshoot; i++)
        {
            Text[i] = 0;
        }

        Text_size = length;
        Sa = new int [length];

        int i, j, ss, sb, k, c1, c2, numQSorted = 0;
        boolean [] bigDone = new boolean [257];
        int [] copyStart = new int [257];
        int [] copyEnd = new int [257];

        // ------ init array containing positions of anchors
        if (Anchor_dist == 0)
        {
            Anchor_num = 0;
        }
        else
        {
            Anchor_num = 2 + (length - 1) / Anchor_dist; // see comment for helped_sort()
            Anchor_rank = new int [Anchor_num];
            Anchor_offset = new int [Anchor_num];
            for (i = 0; i < Anchor_num; i++)
            {
                Anchor_rank[i] = -1; // pos of anchors is initially unknown
                Anchor_offset[i] = Anchor_dist; // maximum possible value
            }
        }

        // ---------- init ftab ------------------
        for (i = 0; i < 66049; i++)
            ftab[i] = 0;
        c1 = Text[0];
        for (i = 1; i <= Text_size; i++)
        {
            c2 = Text[i];
            ftab[(c1 << 8) + c2]++;
            c1 = c2;
        }
        for (i = 1; i < 66049; i++)
            ftab[i] += ftab[i - 1];

        // -------- sort suffixes considering only the first two chars
        c1 = Text[0];
        for (i = 0; i < Text_size; i++)
        {
            c2 = Text[i + 1];
            j = (c1 << 8) + c2;
            c1 = c2;
            ftab[j]--;
            Sa[ftab[j]] = i;
        }

        /* decide on the running order */
        calc_running_order();
        for (i = 0; i < 257; i++)
        {
            bigDone[i] = false;
        }

        /* Really do the suffix sorting */
        for (i = 0; i <= 256; i++)
        {

            /*--
              Process big buckets, starting with the least full.
              --*/
            ss = runningOrder[i];
            /*--
            Complete the big bucket [ss] by sorting
            any unsorted small buckets [ss, j].  Hopefully
            previous pointer-scanning phases have already
            completed many of the small buckets [ss, j], so
            we don't have to sort them at all.
            --*/
            for (j = 0; j <= 256; j++)
            {
                if (j != ss)
                {
                    sb = (ss << 8) + j;
                    if ((ftab[sb] & SETMASK) == 0)
                    {
                        int lo = ftab[sb] & CLEARMASK;
                        int hi = (ftab[sb + 1] & CLEARMASK) - 1;
                        if (hi > lo)
                        {
                            shallow_sort(lo, hi - lo + 1);
                            numQSorted += (hi - lo + 1);
                        }
                    }
                    ftab[sb] |= SETMASK;
                }
            }
            {
                for (j = 0; j <= 256; j++)
                {
                    copyStart[j] = ftab[(j << 8) + ss] & CLEARMASK;
                    copyEnd[j] = (ftab[(j << 8) + ss + 1] & CLEARMASK) - 1;
                }
                // take care of the virtual -1 char in position Text_size+1
                if (ss == 0)
                {
                    k = Text_size - 1;
                    c1 = Text[k];
                    if (!bigDone[c1]) Sa[copyStart[c1]++] = k;
                }
                for (j = ftab[ss << 8] & CLEARMASK; j < copyStart[ss]; j++)
                {
                    k = Sa[j] - 1;
                    if (k < 0) continue;
                    c1 = Text[k];
                    if (!bigDone[c1]) Sa[copyStart[c1]++] = k;
                }
                for (j = (ftab[(ss + 1) << 8] & CLEARMASK) - 1; j > copyEnd[ss]; j--)
                {
                    k = Sa[j] - 1;
                    if (k < 0) continue;
                    c1 = Text[k];
                    if (!bigDone[c1]) Sa[copyEnd[c1]--] = k;
                }
            }
            for (j = 0; j <= 256; j++)
                ftab[(j << 8) + ss] |= SETMASK;
            bigDone[ss] = true;
        }// endfor

        return Sa;
    }

    private void shallow_sort(int a, int n)
    {
        // call multikey quicksort
        // skip 2 chars since suffixes come from the same bucket
        shallow_mkq32(a, n, 2);

    }

    private void shallow_mkq32(int a, int n, int text_depth)
    {

        int partval, val;
        int pa = 0, pb = 0, pc = 0, pd = 0, pl = 0, pm = 0, pn = 0;// pointers
        int d, r;
        int next_depth;// text pointer
        boolean repeatFlag = true;

        // ---- On small arrays use insertions sort
        if (n < Mk_qs_thresh)
        {
            shallow_inssort_lcp(a, n, text_depth);
            return;
        }

        // ----------- choose pivot --------------
        while (repeatFlag)
        {

            repeatFlag = false;
            pl = a;
            pm = a + (n / 2);
            pn = a + (n - 1);
            if (n > 30)
            { // On big arrays, pseudomedian of 9
                d = (n / 8);
                pl = med3(pl, pl + d, pl + 2 * d, text_depth);
                pm = med3(pm - d, pm, pm + d, text_depth);
                pn = med3(pn - 2 * d, pn - d, pn, text_depth);
            }
            pm = med3(pl, pm, pn, text_depth);
            swap2(a, pm);
            partval = ptr2char32(a, text_depth);
            pa = pb = a + 1;
            pc = pd = a + n - 1;
            // -------- partition -----------------
            for (;;)
            {
                while (pb <= pc && (val = ptr2char32(pb, text_depth)) <= partval)
                {
                    if (val == partval)
                    {
                        swap2(pa, pb);
                        pa++;
                    }
                    pb++;
                }
                while (pb <= pc && (val = ptr2char32(pc, text_depth)) >= partval)
                {
                    if (val == partval)
                    {
                        swap2(pc, pd);
                        pd--;
                    }
                    pc--;
                }
                if (pb > pc) break;
                swap2(pb, pc);
                pb++;
                pc--;
            }
            if (pa > pd)
            {
                // all values were equal to partval: make it simpler
                if ((next_depth = text_depth + 4) >= Shallow_limit)
                {
                    helped_sort(a, n, next_depth);
                    return;
                }
                else
                {
                    text_depth = next_depth;
                    repeatFlag = true;
                }
            }

        }
        // partition a[] into the values smaller, equal, and larger that partval
        pn = a + n;
        r = min(pa - a, pb - pa);
        vecswap2(a, pb - r, r);
        r = min(pd - pc, pn - pd - 1);
        vecswap2(pb, pn - r, r);
        // --- sort smaller strings -------
        if ((r = pb - pa) > 1) shallow_mkq32(a, r, text_depth);
        // --- sort strings starting with partval -----
        if ((next_depth = text_depth + 4) < Shallow_limit) shallow_mkq32(a + r, pa - pd
            + n - 1, next_depth);
        else helped_sort(a + r, pa - pd + n - 1, next_depth);
        if ((r = pd - pc) > 1) shallow_mkq32(a + n - r, r, text_depth);

    }

    private void vecswap2(int a, int b, int n)
    {
        while (n-- > 0)
        {
            int t = Sa[a];
            Sa[a++] = Sa[b];
            Sa[b++] = t;
        }
    }

    private static int min(int i, int j)
    {
        return i < j ? i : j;
    }

    /**
     * this is the insertion sort routine called by multikey-quicksort for sorting small
     * groups. During insertion sort the comparisons are done calling
     * cmp_unrolled_shallow_lcp() and two strings are equal if the coincides for
     * Shallow_limit characters. After this first phase we sort groups of "equal_string"
     * using helped_sort(). Usage of lcp. For i=1,...n-1 let lcp[i] denote the lcp between
     * a[i] and a[i+1]. assume a[0] ... a[j-1] are already ordered and that we want to
     * insert a new element ai. If suf(ai) >= suf(a[j-1]) we are done. If
     * suf(ai)<suf(a[j-1]) we notice that: if lcpi>lcp[j-2] then suf(ai)>suf(a[j-2]) and
     * we can stop since j-2 mmmmmmg j-1 mmmmmmmmmmmm ai mmmmmmmmmmmf ] lcpi so we write
     * a[j-1] in position j and ai in position j-1. if lcpi==lcp[j-2] then we need to
     * compare suf(ai) with suf(a[j-2]) j-2 mmmmmmmmmmm? we can have either ?<f or ?>f or
     * ?==f j-1 mmmmmmmmmmmm j mmmmmmmmmmmf so we move a[j-1] to position j and compare
     * suf(ai) with suf(a[j-2]) starting from lcpi. Finally, if lcpi<lcp[j-2] then j-2
     * mmmmmmmmmmmmmmmmmmg j-1 mmmmmmmmmmmmmmmmmmm j mmmmmmmmmmmmmf hence we have
     * suf(ai)<suf(a[j-2]) and we consider a[j-3]; if lcpi<lcp[j-3] we go on look at
     * a[j-4] and go on. if lcp[j]>lcp[j-3] we are in the following position: j-3 mmmmmmc
     * j-2 mmmmmmmmmmmmmmmg j-1 mmmmmmmmmmmmmmmm j mmmmmmmmmmf and we know that suf(ai) is
     * larger than suf(a[j-3]). If we find that lcpi==lcp[j-3] then we must compare
     * suf(ai) with suf(a[j-3]) but starting with position lcpi
     */
    private void shallow_inssort_lcp(int a, int n, int text_depth)
    {
        int i, j, j1, lcp_new, r, ai, lcpi;
        int cmp_from_limit;
        int text_depth_ai;// pointer
        // --------- initialize ----------------

        lcp_aux[0] = -1; // set lcp[-1] = -1
        for (i = 0; i < n; i++)
        {
            lcp_aux[lcp + i] = 0;
        }
        cmp_from_limit = Shallow_limit - text_depth;

        // ----- start insertion sort -----------
        for (i = 1; i < n; i++)
        {
            ai = Sa[a + i];
            lcpi = 0;
            text_depth_ai = ai + text_depth;
            j = i;
            j1 = j - 1; // j1 is a shorhand for j-1
            while (true)
            {

                // ------ compare ai with a[j-1] --------
                Cmp_left = cmp_from_limit - lcpi;
                r = cmp_unrolled_shallow_lcp(lcpi + Sa[a + j1] + text_depth, lcpi
                    + text_depth_ai);
                lcp_new = cmp_from_limit - Cmp_left; // lcp between ai and a[j1]
                assert (r != 0 || lcp_new >= cmp_from_limit);

                if (r <= 0)
                { // we have a[j-1] <= ai
                    lcp_aux[lcp + j1] = lcp_new; // ai will be written in a[j]; update
                    // lcp[j-1]
                    break;
                }

                // --- we have a[j-1]>ai. a[j-1] and maybe other will be moved down
                // --- use lcp to move down as many elements of a[] as possible
                lcpi = lcp_new;
                do
                {
                    Sa[a + j] = Sa[a + j1]; // move down a[j-1]
                    lcp_aux[lcp + j] = lcp_aux[lcp + j1]; // move down lcp[j-1]
                    j = j1;
                    j1--; // update j and j1=j-1
                }
                while (lcpi < lcp_aux[lcp + j1]); // recall that lcp[-1]=-1

                if (lcpi > lcp_aux[lcp + j1]) break; // ai will be written in position j

                // if we get here lcpi==lcp[j1]: we will compare them at next iteration

            } // end for(j=i ...
            Sa[a + j] = ai;
            lcp_aux[lcp + j] = lcpi;
        } // end for(i=1 ...
        // ----- done with insertion sort. now sort groups of equal strings
        for (i = 0; i < n - 1; i = j + 1)
        {
            for (j = i; j < n; j++)
                if (lcp_aux[lcp + j] < cmp_from_limit) break;
            if (j - i > 0) helped_sort(a + i, j - i + 1, Shallow_limit);
        }
    }

    /**
     * Function to compare two strings originating from the *b1 and *b2 The size of the
     * unrolled loop must be at most equal to the costant Cmp_overshoot defined in
     * common.h When the function is called Cmp_left must contain the maximum number of
     * comparisons the algorithm can do before returning 0 (equal strings) At exit
     * Cmp_left has been decreased by the # of comparisons done
     */
    private int cmp_unrolled_shallow_lcp(int b1, int b2)
    {

        int c1, c2;

        // execute blocks of 16 comparisons until a difference
        // is found or we run out of the string
        do
        {
            // 1
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                return c1 - c2;
            }
            b1++;
            b2++;
            // 2
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 1;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 3
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 2;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 4
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 3;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 5
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 4;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 6
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 5;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 7
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 6;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 8
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 7;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 9
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 8;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 10
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 9;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 11
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 10;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 12
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 11;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 13
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 12;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 14
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 13;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 15
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 14;
                return c1 - c2;
            }
            b1++;
            b2++;
            // 16
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_left -= 15;
                return c1 - c2;
            }
            b1++;
            b2++;
            // if we have done enough comparisons the strings are considered equal
            Cmp_left -= 16;
            if (Cmp_left <= 0) return 0;
            // assert( b1<Upper_text_limit && b2<Upper_text_limit);
        }
        while (true);
        // return b2 - b1;
    }

    /**
     * This procedure sort the strings a[0] ... a[n-1] with the help of an anchor. The
     * real sorting is done by the procedure anchor_sort(). Here we choose the anchor. The
     * parameter depth is the number of chars that a[0] ... a[n-1] are known to have in
     * common (thus a direct comparison among a[i] and a[j] should start from position
     * depth) Note that a[] is a subsection of the sa therefore a[0] ... a[n-1] are
     * starting position of suffixes For every a[i] we look at the anchor a[i]/Anchor_dist
     * and the one after that. This justifies the definition of Anchor_num (the size of
     * Anchor_ofset[] and Anchor_rank[] defined in ds_sort()) as Anchor_num = 2 +
     * (n-1)/Anchor_dist
     */
    private void helped_sort(int a, int n, int depth)
    {
        int i, curr_sb, diff, toffset, aoffset;
        int text_pos, anchor_pos, anchor, anchor_rank;
        int min_forw_offset, min_forw_offset_buc, max_back_offset;
        int best_forw_anchor, best_forw_anchor_buc, best_back_anchor;
        int forw_anchor_index, forw_anchor_index_buc, back_anchor_index;

        if (n == 1) return; // simplest case: only one string

        // if there are no anchors use pseudo-anchors or deep_sort
        if (Anchor_dist == 0)
        {
            pseudo_or_deep_sort(a, n, depth);
            return;
        }

        // compute the current bucket
        curr_sb = Get_small_bucket(Sa[a]);

        // init best anchor variables with illegal values
        min_forw_offset = min_forw_offset_buc = Integer.MAX_VALUE;
        max_back_offset = Integer.MIN_VALUE;
        best_forw_anchor = best_forw_anchor_buc = best_back_anchor = -1;
        forw_anchor_index = forw_anchor_index_buc = back_anchor_index = -1;

        // look at the anchor preceeding each a[i]
        for (i = 0; i < n; i++)
        {
            text_pos = Sa[a + i];
            // get anchor preceeding text_pos=a[i]
            anchor = text_pos / Anchor_dist;
            toffset = text_pos % Anchor_dist; // distance of a[i] from anchor
            aoffset = Anchor_offset[anchor]; // distance of sorted suf from anchor
            if (aoffset < Anchor_dist)
            { // check if it is a "sorted" anchor
                diff = aoffset - toffset;
                if (diff > 0)
                { // anchor <= a[i] < (sorted suffix)
                    if (curr_sb != Get_small_bucket(text_pos + diff))
                    {
                        if (diff < min_forw_offset)
                        {
                            min_forw_offset = diff;
                            best_forw_anchor = anchor;
                            forw_anchor_index = i;
                        }
                    }
                    else
                    { // the sorted suffix belongs to the same bucket of a[0]..a[n-1]
                        if (diff < min_forw_offset_buc)
                        {
                            min_forw_offset_buc = diff;
                            best_forw_anchor_buc = anchor;
                            forw_anchor_index_buc = i;
                        }
                    }
                }
                else
                { // diff<0 => anchor <= (sorted suffix) < a[i]
                    if (diff > max_back_offset)
                    {
                        max_back_offset = diff;
                        best_back_anchor = anchor;
                        back_anchor_index = i;
                    }
                    // try to find a sorted suffix > a[i] by looking at next anchor
                    aoffset = Anchor_offset[++anchor];
                    if (aoffset < Anchor_dist)
                    {
                        diff = Anchor_dist + aoffset - toffset;
                        assert (diff > 0);
                        if (curr_sb != Get_small_bucket(text_pos + diff))
                        {
                            if (diff < min_forw_offset)
                            {
                                min_forw_offset = diff;
                                best_forw_anchor = anchor;
                                forw_anchor_index = i;
                            }
                        }
                        else
                        {
                            if (diff < min_forw_offset_buc)
                            {
                                min_forw_offset_buc = diff;
                                best_forw_anchor_buc = anchor;
                                forw_anchor_index_buc = i;
                            }
                        }
                    }
                }
            }
        }

        // ------ if forward anchor_sort is possible, do it! --------
        if (best_forw_anchor >= 0 && min_forw_offset < depth - 1)
        {
            anchor_pos = Sa[a + forw_anchor_index] + min_forw_offset;
            anchor_rank = Anchor_rank[best_forw_anchor];
            general_anchor_sort(a, n, anchor_pos, anchor_rank, min_forw_offset);
            if (Anchor_dist > 0) update_anchors(a, n);
            return;
        }

        boolean fail = false;
        if (best_back_anchor >= 0)
        {
            int T0, Ti;// text pointers
            int j;

            // make sure that the offset is legal for all a[i]
            for (i = 0; i < n; i++)
            {
                if (Sa[a + i] + max_back_offset < 0) fail = true;
                // goto fail; // illegal offset, give up
            }
            // make sure that a[0] .. a[n-1] are preceded by the same substring
            T0 = Sa[a];
            for (i = 1; i < n; i++)
            {
                Ti = Sa[a + i];
                for (j = max_back_offset; j <= -1; j++)
                    if (Text[T0 + j] != Text[Ti + j]) fail = true;
                // goto fail; // mismatch, give up
            }
            if (!fail)
            {
                // backward anchor sorting is possible
                anchor_pos = Sa[a + back_anchor_index] + max_back_offset;
                anchor_rank = Anchor_rank[best_back_anchor];
                general_anchor_sort(a, n, anchor_pos, anchor_rank, max_back_offset);
                if (Anchor_dist > 0) update_anchors(a, n);
                return;
            }
        }
        if (fail)
        {
            if (best_forw_anchor_buc >= 0 && min_forw_offset_buc < depth - 1)
            {
                int equal = 0, lower = 0, upper = 0;

                anchor_pos = Sa[a + forw_anchor_index_buc] + min_forw_offset_buc;
                anchor_rank = Anchor_rank[best_forw_anchor_buc];

                // establish how many suffixes can be sorted using anchor_sort()
                SplitGroupResult res = split_group(a, n, depth, min_forw_offset_buc,
                    forw_anchor_index_buc, lower);
                equal = res.equal;
                lower = res.lower;
                if (equal == n)
                {
                    general_anchor_sort(a, n, anchor_pos, anchor_rank,
                        min_forw_offset_buc);
                }
                else
                {
                    // -- a[0] ... a[n-1] are split into 3 groups: lower, equal, upper
                    upper = n - equal - lower;
                    // printf("Warning! lo=%d eq=%d up=%d a=%x\n",lower,equal,upper,(int)a);
                    // sort the equal group
                    if (equal > 1) general_anchor_sort(a + lower, equal, anchor_pos,
                        anchor_rank, min_forw_offset_buc);

                    // sort upper and lower groups using deep_sort
                    if (lower > 1) pseudo_or_deep_sort(a, lower, depth);
                    if (upper > 1) pseudo_or_deep_sort(a + lower + equal, upper, depth);
                } // end if(equal==n) ... else
                if (Anchor_dist > 0) update_anchors(a, n);
                return;
            } // end hard case

        }
        // ---------------------------------------------------------------
        // If we get here it means that everything failed
        // In this case we simply deep_sort a[0] ... a[n-1]
        // ---------------------------------------------------------------
        pseudo_or_deep_sort(a, n, depth);

    }

    /**
     * This function takes as input an array a[0] .. a[n-1] of suffixes which share the
     * first "depth" chars. "pivot" in an index in 0..n-1 and offset and integer>0. The
     * function splits a[0] .. a[n-1] into 3 groups: first the suffixes which are smaller
     * than a[pivot], then those which are equal to a[pivot] and finally those which are
     * greater than a[pivot]. Here, smaller, equal, larger refer to a lexicographic
     * ordering limited to the first depth+offest chars (since the first depth chars are
     * equal we only look at the chars in position depth, depth+1, ... depth+offset-1).
     * The function returns the number "num" of suffixes equal to a[pivot], and stores in
     * *first the first of these suffixes. So at the end the smaller suffixes are in a[0]
     * ... a[first-1], the equal suffixes in a[first] ... a[first+num-1], the larger
     * suffixes in a[first+num] ... a[n-1] The splitting is done using a modified mkq()
     */
    private SplitGroupResult split_group(int a, int n, int depth, int offset, int pivot,
        int first)
    {
        int r, partval;
        int pa, pb, pc, pd, pa_old, pd_old;// pointers
        int pivot_pos;
        int text_depth, text_limit;// pointers

        // --------- initialization ------------------------------------
        pivot_pos = Sa[a + pivot]; // starting position in T[] of pivot
        text_depth = depth;
        text_limit = text_depth + offset;

        // -------------------------------------------------------------
        // In the following for() loop:
        // [pa ... pd] is the current working region,
        // pb moves from pa towards pd
        // pc moves from pd towards pa
        // -------------------------------------------------------------
        pa = a;
        pd = a + n - 1;

        for (; pa != pd && (text_depth < text_limit); text_depth++)
        {
            // ------ the pivot char is Text[pivot_pos+depth] where
            // depth = text_depth-Text. This is text_depth[pivot_pos]
            partval = Text[text_depth + pivot_pos];
            // ----- partition ------------
            pb = pa_old = pa;
            pc = pd_old = pd;
            for (;;)
            {
                while (pb <= pc && (r = ptr2char(pb, text_depth) - partval) <= 0)
                {
                    if (r == 0)
                    {
                        swap2(pa, pb);
                        pa++;
                    }
                    pb++;
                }
                while (pb <= pc && (r = ptr2char(pc, text_depth) - partval) >= 0)
                {
                    if (r == 0)
                    {
                        swap2(pc, pd);
                        pd--;
                    }
                    pc--;
                }
                if (pb > pc) break;
                swap2(pb, pc);
                pb++;
                pc--;
            }
            r = min(pa - pa_old, pb - pa);
            vecswap2(pa_old, pb - r, r);
            r = min(pd - pc, pd_old - pd);
            vecswap2(pb, pd_old + 1 - r, r);
            // ------ compute new boundaries -----
            pa = pa_old + (pb - pa); // there are pb-pa chars < partval
            pd = pd_old - (pd - pc); // there are pd-pc chars > partval

        }

        first = pa - a; // index in a[] of the first suf. equal to pivot
        // return pd-pa+1; // return number of suffixes equal to pivot
        return new SplitGroupResult(pd - pa + 1, first);

    }

    /**
     * given a SORTED array of suffixes a[0] .. a[n-1] updates Anchor_rank[] and
     * Anchor_offset[]
     */
    private void update_anchors(int a, int n)
    {
        int i, anchor, toffset, aoffset, text_pos;

        for (i = 0; i < n; i++)
        {
            text_pos = Sa[a + i];
            // get anchor preceeding text_pos=a[i]
            anchor = text_pos / Anchor_dist;
            toffset = text_pos % Anchor_dist; // distance of a[i] from anchor
            aoffset = Anchor_offset[anchor]; // dist of sorted suf from anchor
            if (toffset < aoffset)
            {
                Anchor_offset[anchor] = toffset;
                Anchor_rank[anchor] = a + i;
            }
        }

    }

    /**
     * This routines sorts a[0] ... a[n-1] using the fact that in their common prefix,
     * after offset characters, there is a suffix whose rank is known. In this routine we
     * call this suffix anchor (and we denote its position and rank with anchor_pos and
     * anchor_rank respectively) but it is not necessarily an anchor (=does not
     * necessarily starts at position multiple of Anchor_dist) since this function is
     * called by pseudo_anchor_sort(). The routine works by scanning the suffixes before
     * and after the anchor in order to find (and mark) those which are suffixes of a[0]
     * ... a[n-1]. After that, the ordering of a[0] ... a[n-1] is derived with a sigle
     * scan of the marked
     * suffixes.*******************************************************************
     */
    private void general_anchor_sort(int a, int n, int anchor_pos, int anchor_rank,
        int offset)
    {
        int sb, lo, hi;
        int curr_lo, curr_hi, to_be_found, i, j;
        int item;
        int ris;
        // void *ris;

        /* ---------- get bucket of anchor ---------- */
        sb = Get_small_bucket(anchor_pos);
        lo = BUCKET_FIRST(sb);
        hi = BUCKET_LAST(sb);
        // ------ sort pointers a[0] ... a[n-1] as plain integers
        // qsort(a, n, sizeof(Int32), integer_cmp);
        Arrays.sort(Sa, a, a + n);

        // ------------------------------------------------------------------
        // now we scan the bucket containing the anchor in search of suffixes
        // corresponding to the ones we have to sort. When we find one of
        // such suffixes we mark it. We go on untill n sfx's have been marked
        // ------------------------------------------------------------------
        curr_hi = curr_lo = anchor_rank;

        MARK(curr_lo);
        // scan suffixes preceeding and following the anchor
        for (to_be_found = n - 1; to_be_found > 0;)
        {
            // invariant: the next positions to check are curr_lo-1 and curr_hi+1
            assert (curr_lo > lo || curr_hi < hi);
            while (curr_lo > lo)
            {
                item = Sa[--curr_lo] - offset;
                ris = Arrays.binarySearch(Sa, a, a + n, item);
                // ris = bsearch(&item,a,n,sizeof(Int32), integer_cmp);
                if (ris != 0)
                {
                    MARK(curr_lo);
                    to_be_found--;
                }
                else break;
            }
            while (curr_hi < hi)
            {
                item = Sa[++curr_hi] - offset;
                ris = Arrays.binarySearch(Sa, a, a + n, item);
                if (ris != 0)
                {
                    MARK(curr_hi);
                    to_be_found--;
                }
                else break;
            }
        }
        // sort a[] using the marked suffixes
        for (j = 0, i = curr_lo; i <= curr_hi; i++)
            if (ISMARKED(i))
            {
                UNMARK(i);
                Sa[a + j++] = Sa[i] - offset;
            }

    }

    /**
     * 
     */
    private void UNMARK(int i)
    {
        Sa[i] &= ~MARKER;

    }

    /**
     * 
     */
    private boolean ISMARKED(int i)
    {
        return (Sa[i] & MARKER) != 0;
    }

    /**
     */
    private void MARK(int i)
    {
        Sa[i] |= MARKER;

    }

    /**
     * 
     */
    private int BUCKET_LAST(int sb)
    {
        return (ftab[sb + 1] & CLEARMASK) - 1;
    }

    /**
     * 
     */
    private int BUCKET_FIRST(int sb)
    {
        return ftab[sb] & CLEARMASK;
    }

    /**
     * 
     */
    private int BUCKET_SIZE(int sb)
    {
        return (ftab[sb + 1] & CLEARMASK) - (ftab[sb] & CLEARMASK);
    }

    /**
     * 
     */
    private int Get_small_bucket(int pos)
    {
        return (Text[pos] << 8) + Text[pos + 1];
    }

    /**
     * 
     */
    private void pseudo_or_deep_sort(int a, int n, int depth)
    {
        int offset, text_pos, sb, pseudo_anchor_pos, max_offset, size;

        // ------- search for a useful pseudo-anchor -------------
        if (Max_pseudo_anchor_offset > 0)
        {

            max_offset = min(depth - 1, Max_pseudo_anchor_offset);
            text_pos = Sa[a];
            for (offset = 1; offset < max_offset; offset++)
            {
                pseudo_anchor_pos = text_pos + offset;
                sb = Get_small_bucket(pseudo_anchor_pos);
                // check if pseudo_anchor is in a sorted bucket
                if (IS_SORTED_BUCKET(sb))
                {
                    size = BUCKET_SIZE(sb); // size of group
                    if (size > B2g_ratio * n) continue; // discard large groups
                    // sort a[0] ... a[n-1] using pseudo_anchor
                    pseudo_anchor_sort(a, n, pseudo_anchor_pos, offset);
                    return;
                }
            }
        }
        deep_sort(a, n, depth);

    }

    /**
     * 
     */
    private boolean IS_SORTED_BUCKET(int sb)
    {
        return (ftab[sb] & SETMASK) != 0;
    }

    /**
     * routine for deep-sorting the suffixes a[0] ... a[n-1] knowing that they have a
     * common prefix of length "depth"
     */
    private void deep_sort(int a, int n, int depth)
    {
        int blind_limit;

        blind_limit = Text_size / Blind_sort_ratio;
        if (n <= blind_limit) blind_ssort(a, n, depth); // small_group
        else qs_unrolled_lcp(a, n, depth, blind_limit);

    }

    /**
     * ternary quicksort (seward-like) with lcp information
     */
    private void qs_unrolled_lcp(int a, int n, int depth, int blind_limit)
    {
        int text_depth, text_pos_pivot;// pointers
        int [] stack_lo = new int [STACK_SIZE];
        int [] stack_hi = new int [STACK_SIZE];
        int [] stack_d = new int [STACK_SIZE];
        int sp, r, r3, med;
        int i, j, lo, hi, ris, lcp_lo, lcp_hi;
        // ----- init quicksort --------------
        r = sp = 0;
        // Pushd(0,n-1,depth);
        stack_lo[sp] = 0;
        stack_hi[sp] = n - 1;
        stack_d[sp] = depth;
        sp++;
        // end Pushd

        // ----- repeat untill stack is empty ------
        while (sp > 0)
        {
            assert (sp < STACK_SIZE);
            // Popd(lo,hi,depth);
            sp--;
            lo = stack_lo[sp];
            hi = stack_hi[sp];
            depth = stack_d[sp];
            // end popd
            text_depth = depth;

            // --- use shellsort for small groups
            if (hi - lo < blind_limit)
            {
                blind_ssort(a + lo, hi - lo + 1, depth);
                continue;
            }

            /*
             * Random partitioning. Guidance for the magic constants 7621 and 32768 is
             * taken from Sedgewick's algorithms book, chapter 35.
             */
            r = ((r * 7621) + 1) % 32768;
            r3 = r % 3;
            if (r3 == 0) med = lo;
            else if (r3 == 1) med = (lo + hi) >> 1;
            else med = hi;

            // --- partition ----
            Swap(med, hi, a); // put the pivot at the right-end
            text_pos_pivot = text_depth + Sa[a + hi];
            i = lo - 1;
            j = hi;
            lcp_lo = lcp_hi = Integer.MAX_VALUE;
            while (true)
            {
                while (++i < hi)
                {
                    ris = cmp_unrolled_lcp(text_depth + Sa[a + i], text_pos_pivot);
                    if (ris > 0)
                    {
                        if (Cmp_done < lcp_hi) lcp_hi = Cmp_done;
                        break;
                    }
                    else if (Cmp_done < lcp_lo) lcp_lo = Cmp_done;
                }
                while (--j > lo)
                {
                    ris = cmp_unrolled_lcp(text_depth + Sa[a + j], text_pos_pivot);
                    if (ris < 0)
                    {
                        if (Cmp_done < lcp_lo) lcp_lo = Cmp_done;
                        break;
                    }
                    else if (Cmp_done < lcp_hi) lcp_hi = Cmp_done;
                }
                if (i >= j) break;
                Swap(i, j, a);
            }
            Swap(i, hi, a); // put pivot at the middle

            // --------- insert subproblems in stack; smallest last
            if (i - lo < hi - i)
            {
                // Pushd(i + 1, hi, depth + lcp_hi);
                stack_lo[sp] = i + 1;
                stack_hi[sp] = hi;
                stack_d[sp] = depth + lcp_hi;
                sp++;
                // end pushd
                if (i - lo > 1)
                {
                    // Pushd(lo, i - 1, depth + lcp_lo);
                    stack_lo[sp] = lo;
                    stack_hi[sp] = i - 1;
                    stack_d[sp] = depth + lcp_lo;
                    sp++;
                    // end push
                }

            }
            else
            {
                // Pushd(lo, i - 1, depth + lcp_lo);
                stack_lo[sp] = lo;
                stack_hi[sp] = i - 1;
                stack_d[sp] = depth + lcp_lo;
                sp++;
                // end pushd
                if (hi - i > 1)
                {
                    // Pushd(i + 1, hi, depth + lcp_hi);
                    stack_lo[sp] = i + 1;
                    stack_hi[sp] = hi;
                    stack_d[sp] = depth + lcp_hi;
                    sp++;
                    // end pushd
                }
            }
        }

    }

    /**
     * Function to compare two strings originating from the *b1 and *b2 The size of the
     * unrolled loop must be at most equal to the costant Cmp_overshoot defined in
     * common.h the function return the result of the comparison (+ or -) and writes in
     * Cmp_done the number of successfull comparisons done
     */
    private int cmp_unrolled_lcp(int b1, int b2)
    {

        int c1, c2;
        Cmp_done = 0;

        // execute blocks of 16 comparisons untill a difference
        // is found or we run out of the string
        do
        {
            // 1
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 2
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 1;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 3
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 2;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 4
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 3;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 5
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 4;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 6
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 5;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 7
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 6;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 8
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 7;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 9
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 8;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 10
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 9;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 11
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 10;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 12
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 11;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 13
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 12;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 14
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 13;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 15
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 14;
                return (c1 - c2);
            }
            b1++;
            b2++;
            // 16
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                Cmp_done += 15;
                return (c1 - c2);
            }
            b1++;
            b2++;

            Cmp_done += 16;

        }
        while (b1 < Text_size && b2 < Text_size);

        return b2 - b1;

    }

    private void Swap(int i, int j, int a)
    {
        int tmp = Sa[a + i];
        Sa[a + i] = Sa[a + j];
        Sa[a + j] = tmp;
    }

    /**
     * routine for deep-sorting the suffixes a[0] ... a[n-1] knowing that they have a
     * common prefix of length "depth"
     */
    private void blind_ssort(int a, int n, int depth)
    {
        int i, j, aj, lcp;
        Node nh, root, h;

        // ---- sort suffixes in order of increasing length
        // qsort(a, n, sizeof(Int32), neg_integer_cmp);
        Arrays.sort(Sa, a, a + n);
        for (int left = 0, right = n - 1; left < right; left++, right--)
        {
            // exchange the first and last
            int temp = Sa[a + left];
            Sa[a + left] = Sa[a + right];
            Sa[a + right] = temp;
        }

        // --- skip suffixes which have already reached the end-of-text
        for (j = 0; j < n; j++)
            if (Sa[a + j] + depth < Text_size) break;
        if (j >= n - 1) return; // everything is already sorted!

        // ------ init stack -------
        // Stack = (node **) malloc(n*sizeof(node *));

        // ------- init root with the first unsorted suffix
        nh = new Node();
        nh.skip = -1;
        nh.right = null;
        // nh.down = (void *) a[j];
        nh.downInt = Sa[a + j];
        root = nh;

        // ------- insert suffixes a[j+1] ... a[n-1]
        for (i = j + 1; i < n; i++)
        {
            h = find_companion(root, Sa[a + i]);
            aj = h.downInt;
            lcp = compare_suffixes(aj, Sa[a + i], depth);
            insert_suffix(root, Sa[a + i], lcp, Text[aj + lcp]);
        }

        // ---- traverse the trie and get suffixes in lexicographic order
        Aux = a;
        Aux_written = j;
        traverse_trie(root);

    }

    /**
     * this procedures traverse the trie in depth first order so that the suffixes (stored
     * in the leaf) are recovered in lexicographic order
     */
    private void traverse_trie(Node h)
    {
        Node p, nextp;

        if (h.skip < 0) Sa[Aux + Aux_written++] = h.downInt;
        else
        {
            p = h.down;
            do
            {
                nextp = p.right;
                if (nextp != null)
                {
                    // if there are 2 nodes with equal keys
                    // they must be considered in inverted order
                    if (nextp.key == p.key)
                    {
                        traverse_trie(nextp);
                        traverse_trie(p);
                        p = nextp.right;
                        continue;
                    }
                }
                traverse_trie(p);
                p = nextp;
            }
            while (p != null);
        }

    }

    /**
     * insert a suffix in the trie rooted at *p. we know that the trie already contains a
     * string which share the first n chars with suf
     */
    private void insert_suffix(Node h, int suf, int n, int mmchar)
    {
        int c, s;
        Node p, pp;

        s = suf;

        // --------- insert a new node before node *h if necessary
        if (h.skip != n)
        {
            p = new_node__blind_ssort(); // create and init new node
            p.key = mmchar;
            p.skip = h.skip; // p inherits skip and children of *h
            p.down = h.down;
            p.downInt = h.downInt;
            p.right = null;
            h.skip = n;
            h.down = p; // now *h has p as the only child
        }

        // -------- search the position of s[n] among *h offsprings
        c = Text[s + n];
        pp = h.down;
        while (pp != null)
        {
            if (pp.key >= c) break;
            pp = pp.right;
        }
        // ------- insert new node containing suf
        p = new_node__blind_ssort();
        p.skip = -1;
        p.key = c;
        p.right = pp;
        pp = p;
        p.downInt = suf;
        return;

    }

    private Node new_node__blind_ssort()
    {
        return new Node();
    }

    /**
     * this function returns the lcp between suf1 and suf2 (that is returns n such that
     * suf1[n]!=suf2[n] but suf1[i]==suf2[i] for i=0..n-1 However, it is possible that
     * suf1 is a prefix of suf2 (not vice-versa because of the initial sorting of suffixes
     * in order of descreasing length) in this case the function returns n=length(suf1)-1.
     * So in this case suf1[n]==suf2[n] (and suf1[n+1] does not exists).
     */
    private int compare_suffixes(int suf1, int suf2, int depth)
    {
        int limit;
        int s1, s2;

        s1 = depth + suf1;
        s2 = depth + suf2;
        limit = Text_size - suf1 - depth;
        return depth + get_lcp_unrolled(s1, s2, limit);
    }

    /**
     * 
     */
    private int get_lcp_unrolled(int b1, int b2, int cmp_limit)
    {
        int cmp2do;
        int c1, c2;

        // execute blocks of 16 comparisons untill a difference
        // is found or we reach cmp_limit comparisons
        cmp2do = cmp_limit;
        do
        {
            // 1
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                break;
            }
            b1++;
            b2++;
            // 2
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 1;
                break;
            }
            b1++;
            b2++;
            // 3
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 2;
                break;
            }
            b1++;
            b2++;
            // 4
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 3;
                break;
            }
            b1++;
            b2++;
            // 5
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 4;
                break;
            }
            b1++;
            b2++;
            // 6
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 5;
                break;
            }
            b1++;
            b2++;
            // 7
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 6;
                break;
            }
            b1++;
            b2++;
            // 8
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 7;
                break;
            }
            b1++;
            b2++;
            // 9
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 8;
                break;
            }
            b1++;
            b2++;
            // 10
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 9;
                break;
            }
            b1++;
            b2++;
            // 11
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 10;
                break;
            }
            b1++;
            b2++;
            // 12
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 11;
                break;
            }
            b1++;
            b2++;
            // 13
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 12;
                break;
            }
            b1++;
            b2++;
            // 14
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 13;
                break;
            }
            b1++;
            b2++;
            // 15
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 14;
                break;
            }
            b1++;
            b2++;
            // 16
            c1 = Text[b1];
            c2 = Text[b2];
            if (c1 != c2)
            {
                cmp2do -= 15;
                break;
            }
            b1++;
            b2++;

            cmp2do -= 16;
        }
        while (cmp2do > 0);

        if (cmp_limit - cmp2do < cmp_limit) return cmp_limit - cmp2do;

        return cmp_limit - 1;
    }

    /**
     * this function traverses the trie rooted at head following the string s. Returns the
     * leaf "corresponding" to the string s
     */
    private Node find_companion(Node head, int s)
    {
        int c;
        Node p;
        int t;

        Stack_size = 0; // init stack
        while (head.skip >= 0)
        {
            Stack[Stack_size++] = head;
            t = head.skip;
            if (s + t >= Text_size) // s[t] does not exist: mismatch
            return get_leaf(head);
            c = Text[s + t];
            p = head.down;
            boolean repeat = true;
            while (repeat)
            {
                if (c == p.key)
                { // found branch corresponding to c
                    head = p;
                    repeat = false;
                }
                else if (c < p.key) // no branch corresponding to c: mismatch
                {
                    return get_leaf(head);
                }
                if (repeat && (p = (p.right)) == null) // no other branches: mismatch
                {
                    return get_leaf(head);
                }
            }
        }
        Stack[Stack_size++] = head;
        return head;
    }

    // this function returns a leaf below "head".
    // any leaf will do for the algorithm: we take the easiest to reach
    private Node get_leaf(Node head)
    {
        Tools.assertAlways(head.skip >= 0, "");
        do
        {
            head = head.down;
        }
        while (head.skip >= 0);
        return head;
    }

    /**
     * 
     */
    private void pseudo_anchor_sort(int a, int n, int pseudo_anchor_pos, int offset)
    {
        int pseudo_anchor_rank;

        // ---------- compute rank ------------
        if (Update_anchor_ranks != 0 && Anchor_dist > 0) pseudo_anchor_rank = get_rank_update_anchors(pseudo_anchor_pos);
        else pseudo_anchor_rank = get_rank(pseudo_anchor_pos);
        // ---------- check rank --------------
        assert (Sa[pseudo_anchor_rank] == pseudo_anchor_pos);
        // ---------- do the sorting ----------
        general_anchor_sort(a, n, pseudo_anchor_pos, pseudo_anchor_rank, offset);

    }

    /**
     * compute the rank of the suffix starting at pos. It is required that the suffix is
     * in an already sorted bucket
     */
    private int get_rank(int pos)
    {
        int sb, lo, hi, j;

        sb = Get_small_bucket(pos);
        if (!IS_SORTED_BUCKET(sb))
        {
            throw new RuntimeException("Illegal call to get_rank! (get_rank1)");
        }
        lo = BUCKET_FIRST(sb);
        hi = BUCKET_LAST(sb);
        for (j = lo; j <= hi; j++)
            if (Sa[j] == pos) return j;
        throw new RuntimeException("Illegal call to get_rank! (get_rank2)");
    }

    /**
     * compute the rank of the suffix starting at pos. At the same time check if the rank
     * of the suffixes in the bucket containing pos can be used to update some entries in
     * Anchor_offset[] and Anchor_rank[] It is required that the suffix is in an already
     * sorted bucket
     */
    private int get_rank_update_anchors(int pos)
    {
        int sb, lo, hi, j, toffset, aoffset, anchor, rank;

        // --- get bucket and verify it is a sorted one
        sb = Get_small_bucket(pos);
        if (!(IS_SORTED_BUCKET(sb)))
        {
            throw new RuntimeException(
                "Illegal call to get_rank! (get_rank_update_anchors)");
        }
        // --- if the bucket has been already ranked just compute rank;
        if (bucket_ranked[sb] != 0) return get_rank(pos);
        // --- rank all the bucket
        bucket_ranked[sb] = 1;
        rank = -1;
        lo = BUCKET_FIRST(sb);
        hi = BUCKET_LAST(sb);
        for (j = lo; j <= hi; j++)
        {
            // see if we can update an anchor
            toffset = Sa[j] % Anchor_dist;
            anchor = Sa[j] / Anchor_dist;
            aoffset = Anchor_offset[anchor]; // dist of sorted suf from anchor
            if (toffset < aoffset)
            {
                Anchor_offset[anchor] = toffset;
                Anchor_rank[anchor] = j;
            }
            // see if we have found the rank of pos, if so store it in rank
            if (Sa[j] == pos)
            {
                assert (rank == -1);
                rank = j;
            }
        }
        assert (rank >= 0);
        return rank;
    }

    private void swap2(int a, int b)
    {
        int tmp = Sa[a];
        Sa[a] = Sa[b];
        Sa[b] = tmp;

    }

    /*
     * #define ptr2char32(i) (getword32(*(i) + text_depth))
     */

    private int ptr2char32(int a, int depth)
    {
        return getword32(Sa[a] + depth);
    }

    /*
     * #define getword32(s) ((unsigned)( (*(s) << 24) | ((*((s)+1)) << 16) \ | ((*((s)+2))
     * << 8) | (*((s)+3)) ))
     */
    private int getword32(int s)
    {
        return Text[s] << 24 | Text[s + 1] << 16 | Text[s + 2] << 8 | Text[s + 3];
    }

    private int ptr2char(int i, int text_depth)
    {
        return Text[Sa[i] + text_depth];
    }

    private int med3(int a, int b, int c, int depth)
    {
        int va = ptr2char(a, depth);
        int vb = ptr2char(b, depth);
        if (va == vb)
        {
            return a;
        }
        int vc = ptr2char(c, depth);
        if (vc == va || vc == vb)
        {
            return c;
        }
        return va < vb ? (vb < vc ? b : (va < vc ? c : a)) : (vb > vc ? b : (va < vc ? a
            : c));
    }

    private void calc_running_order()
    {
        int i, j;
        for (i = 0; i <= 256; i++)
            runningOrder[i] = i;
        {
            int vv;
            int h = 1;
            do
                h = 3 * h + 1;
            while (h <= 257);
            do
            {
                h = h / 3;
                for (i = h; i <= 256; i++)
                {
                    vv = runningOrder[i];
                    j = i;
                    while (BIGFREQ(runningOrder[j - h]) > BIGFREQ(vv))
                    {
                        runningOrder[j] = runningOrder[j - h];
                        j = j - h;
                        if (j <= (h - 1)) break;
                    }
                    runningOrder[j] = vv;
                }
            }
            while (h != 1);
        }
    }

    /**
     * 
     */
    private int BIGFREQ(int b)
    {
        return ftab[((b) + 1) << 8] - ftab[(b) << 8];
    }
}
