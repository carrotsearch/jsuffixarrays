package org.jsuffixarrays;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test methods for {@link Traversals}.
 */
public class TraversalsTest
{
    /**
     * Test post-order walking of the virtual suffix tree.
     */
    @Test
    public void preorder()
    {
        final String input = "mississippi$";
        final SuffixData sd = SuffixArrays.createWithLCP(input);

        final String [] expected = {
            "$",
            "i$",
            "ippi$",
            "issippi$",
            "ississippi$",
            "issi",
            "i",
            "mississippi$",
            "pi$",
            "ppi$",
            "p",
            "sippi$",
            "sissippi$",
            "si",
            "ssippi$",
            "ssissippi$",
            "ssi",
            "s",
            "" /* root */
        };

        final ArrayList<String> actual = new ArrayList<String>();

        Traversals.postorder(input.length(), sd.getSuffixArray(), sd.getLCP(),
            new Traversals.IPostOrderVisitor()
            {
                public void visitNode(int start, int length)
                {
                    actual.add(input.subSequence(start, start + length).toString());
                }
            });

        Assert.assertEquals(Arrays.asList(expected), actual);
    }
}
