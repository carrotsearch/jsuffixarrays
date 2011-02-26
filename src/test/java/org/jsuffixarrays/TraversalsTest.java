package org.jsuffixarrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test methods for {@link Traversals}.
 */
public class TraversalsTest
{
    /**
     * Test post-order walking of the virtual suffix tree.
     */
    @Test
    public void postorderTest()
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
                public void visitNode(int start, int length, boolean leaf)
                {
                    final String label = input.subSequence(start, start + length).toString(); 
                    Assert.assertTrue(leaf == label.endsWith("$"));
                    actual.add(label);
                }
            });

        Assert.assertEquals(Arrays.asList(expected), actual);
    }
    
    /**
     * Border cases for post-order traversal.
     */
    @Test
    public void postorderEmptyInput()
    {
        // Empty input? No root, no nothing.
        final SuffixData sd = SuffixArrays.createWithLCP("");
        Traversals.postorder(0, sd.getSuffixArray(), sd.getLCP(),
            new Traversals.IPostOrderVisitor()
            {
                public void visitNode(int start, int length, boolean leaf)
                {
                    Assert.fail();
                }
            });
    }

    /**
     * Test post-order traversal with results aggregation function (here: leaf count
     * for internal nodes).
     */
    @Test
    public void postorderLeafCount()
    {
        /*
         * Add repeated substrings separated by unique symbols.
         */
        final String input = "0123.0123;4123,4235$";
        final List<String> expected = new ArrayList<String>(Arrays.asList(new String [] {
            "0123 [2]",
            "123 [3]",
            "23 [4]",
            "3 [4]",
            "4 [2]",
        }));

        // Compare sorted order first.
        Collections.sort(expected);
        final ArrayList<String> actual = new ArrayList<String>();

        final SuffixData sd = SuffixArrays.createWithLCP(input);
        Traversals.postorder(input.length(), sd.getSuffixArray(), sd.getLCP(), 0,
            new Traversals.IPostOrderComputingVisitor<Integer>()
            {
                public Integer aggregate(Integer value1, Integer value2)
                {
                    return value1 + value2;
                }

                public Integer leafValue(int saIndex, int symbolIndex, int length)
                {
                    return 1;
                }

                public void visitNode(int start, int length, boolean leaf, Integer value)
                {
                    final String label = input.subSequence(start, start + length).toString();
                    if (label.matches("[0-9]+"))
                        actual.add(label + " [" + value + "]");
                }
            });

        Assert.assertEquals(expected, actual);
    }
}
