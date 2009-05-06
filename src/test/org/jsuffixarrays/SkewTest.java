package org.jsuffixarrays;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link Skew}.
 */
public class SkewTest extends SuffixArrayBuilderTestBase
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
        return new Skew();
    }

    /**
     * Run a simple test that compares the output of this routine against the original
     * Karkkainen and Sanders code (<code>tryall.C</code>).
     * 
     * @see TryAll
     */
    @Test
    @SuppressWarnings("unchecked")
    public void compareAgainstOriginalOutput() throws IOException
    {
        final StringWriter sw = new StringWriter();
        final TryAll t = new TryAll(sw);
        t.main(5, 5);

        // Compare outputs, line by line.
        List<String> is = IOUtils.readLines(new StringReader(sw.toString()));
        List<String> expected = IOUtils.readLines(getClass().getResourceAsStream(
            "KarkkainenSanders.result-5-5"));

        assertEquals(expected, is);
    }

    /**
     * Karkkainen and Sanders' <code>tryall.C</code>, ported to Java.
     */
    private final static class TryAll
    {
        private final Writer cout;

        TryAll(Writer w)
        {
            this.cout = w;
        }

        private void printV(int [] a, int n, String comment)
        {
            try
            {
                cout.write(comment);
                cout.write(":");

                for (int i = 0; i < n; i++)
                {
                    cout.write(Integer.toString(a[i]));
                    cout.write(" ");
                }
                cout.write("\n");
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        /*
         * 
         */
        private boolean isPermutation(int [] SA, int n)
        {
            boolean [] seen = new boolean [n];
            for (int i = 0; i < n; i++)
                seen[SA[i]] = true;
            for (int i = 0; i < n; i++)
                if (!seen[i]) return false;
            return true;
        }

        /*
         * 
         */
        private boolean sleq(int [] s1, int s1i, int [] s2, int s2i)
        {
            do
            {
                if (s1[s1i] < s2[s2i]) return true;
                if (s1[s1i] > s2[s2i]) return false;
                s2i++;
                s1i++;
            }
            while (true);
        }

        // Is SA a sorted suffix array for s?
        private boolean isSorted(int [] SA, int [] s, int n)
        {
            for (int i = 0; i < n - 1; i++)
            {
                if (!sleq(s, SA[i], s, SA[i + 1])) return false;
            }
            return true;
        }

        // Try all inputs from {1,..,b}^n for 1 <= n <= nmax.
        public void main(int nmax, int b) throws IOException
        {
            // try all strings from (1..b)^n
            for (int n = 2; n <= nmax; n++)
            {
                cout.write(Integer.toString(n));
                cout.write("\n");

                int N = (int) (Math.pow(b, n) + 0.5);
                int [] s = new int [n + 3];
                int [] SA = new int [n + 3];

                for (int i = 0; i < n; i++)
                    s[i] = SA[i] = 1;
                s[n] = s[n + 1] = s[n + 2] = SA[n] = SA[n + 1] = SA[n + 2] = 0;

                for (int j = 0; j < N; j++)
                {
                    printV(s, n, "s");
                    Skew.suffixArray(s, SA, n, b, 0);
                    Assert.assertTrue(s[n] == 0);
                    Assert.assertTrue(s[n + 1] == 0);
                    Assert.assertTrue(SA[n] == 0);
                    Assert.assertTrue(SA[n + 1] == 0);
                    Assert.assertTrue(isPermutation(SA, n));
                    Assert.assertTrue(isSorted(SA, s, n));
                    printV(SA, n, "SA");

                    // generate next s
                    int i;
                    for (i = 0; s[i] == b; i++)
                        s[i] = 1;
                    s[i]++;
                }
            }
        }
    }
}