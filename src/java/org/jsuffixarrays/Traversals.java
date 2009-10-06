package org.jsuffixarrays;

import org.carrot2.util.collect.primitive.IntQueue;

/**
 * Suffix array traversal routines (emulating corresponding suffix tree traversals).
 */
public final class Traversals
{
    /**
     * Visitor interface for post-order traversal methods in {@link Traversals}.
     */
    public interface IPostOrderVisitor
    {
        /**
         * Visits a node in the (virtual) suffix tree, labeled with <code>length</code>
         * objects starting at <code>start</code> in the input sequence.
         * 
         * @param start The node label's starting offset in the input sequence.
         * @param length The node label's length (number of symbols).
         * @param leaf <code>true</code> if this node is a leaf.
         */
        public void visitNode(int start, int length, boolean leaf);
    }

    /**
     * <p>
     * Post-order traversal of all branching nodes in a suffix tree (emulated using a
     * suffix array and the LCP array). Post-order traversal is also called <i>bottom-up
     * traversal</i> that is child nodes are reported before parent nodes (and the root is
     * the last node to process).
     * <p>
     * The algorithm implemented here is from <i>Efficient Substring Traversal with Suffix
     * Arrays</i> by Toru Kasai, Hiroki Arimura and Setsuo Arikawa, Dept of Informatics,
     * Kyushu University, Japan.
     * 
     * @param sequenceLength Input sequence length for the suffix array and LCP array.
     * @param sa Suffix array.
     * @param lcp Corresponding LCP array for a given suffix array.
     * @param visitor Callback visitor.
     */
    public static void postorder(final int sequenceLength, int [] sa, int [] lcp,
        IPostOrderVisitor visitor)
    {
        assert sequenceLength <= sa.length && sequenceLength <= lcp.length : "Input sequence length larger than suffix array or the LCP.";

        final IntQueue stack = new IntQueue();

        // Push the stack bottom marker (sentinel).
        stack.push(-1, -1);

        // Process every leaf.
        int top_h;
        for (int i = 0; i <= sequenceLength; i++)
        {
            final int h = (sequenceLength == i ? -1 : lcp[i]);

            while (true)
            {
                top_h = stack.get(stack.size() - 1);
                if (top_h <= h) break;

                // Visit the node and remove it from the end of the stack.
                final int top_i = stack.get(stack.size() - 2);
                final boolean leaf = (top_i < 0); 
                stack.pop(2);

                visitor.visitNode(sa[leaf ? -(top_i + 1): top_i], top_h, leaf);
            }

            if (top_h < h)
            {
                stack.push(i, h);
            }

            if (i < sequenceLength)
            {
                // Mark leaf nodes in the stack.
                stack.push(-(i + 1), sequenceLength - sa[i]);
            }
        }
    }
}
