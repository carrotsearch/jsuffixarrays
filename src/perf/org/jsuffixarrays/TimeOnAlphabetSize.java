package org.jsuffixarrays;

import static org.apache.commons.lang.SystemUtils.*;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.kohsuke.args4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measure time taken to build a suffix array on random input of constant size, but with
 * varying alphabet (LCP).
 */
public class TimeOnAlphabetSize
{
    private final Logger logger = LoggerFactory.getLogger("results");

    @Option(aliases =
    {
        "--random-seed"
    }, metaVar = "seed", name = "-rnd", required = false, usage = "Random seed")
    public int randomSeed = 0x11223344;

    @Option(aliases =
    {
        "--start-alphabet"
    }, metaVar = "int", name = "-s", required = false, usage = "starting alphabet size")
    public int startAlphabet = 1;

    @Option(aliases =
    {
        "--increment"
    }, metaVar = "int", name = "-i", required = false, usage = "Alphabet size increment")
    public int increment = 1;

    @Option(aliases =
    {
        "--rounds"
    }, metaVar = "int", name = "-r", required = false, usage = "Number of rounds")
    public int rounds = 100;

    @Option(aliases =
    {
        "--samples"
    }, metaVar = "int", name = "-m", required = false, usage = "Samples taken for each input round")
    public int samples = 1;

    @Option(aliases =
    {
        "--warmup-rounds"
    }, metaVar = "int", name = "-w", required = false, usage = "Warmup rounds")
    public int warmup = 10;

    @Option(aliases =
    {
        "--extra-cells"
    }, metaVar = "int", name = "-e", required = false, usage = "Extra allocated input cells (default: "
        + SuffixArrays.MAX_EXTRA_TRAILING_SPACE + ")")
    public int extraCells = SuffixArrays.MAX_EXTRA_TRAILING_SPACE;

    @Option(aliases =
    {
        "--input-size"
    }, metaVar = "int", name = "-a", required = false, usage = "Input size (>= 1)")
    public int inputSize = 1000000;

    @Option(aliases =
    {
        "--output-file"
    }, metaVar = "file", name = "-o", required = false, usage = "Output file (if not given, stdout is used)")
    public File output;

    @Argument(index = 0, required = true, metaVar = "algorithm", usage = "Algorithm to test (Algorithm class constant).")
    public Algorithm algorithm;

    /*
     * Run the performance test.
     */
    private void run() throws IOException
    {
        Tools.assertAlways(inputSize >= 1, "alphabet size must be >= 1");
        Tools.assertAlways(startAlphabet > 0, "start must be > 0");
        Tools.assertAlways(increment >= 0, "increment must be >= 0");
        Tools.assertAlways(extraCells >= 0, "extra cells must be >= 0");

        final Random rnd = new Random(randomSeed);

        PrintStream out = System.out;
        if (output != null)
        {
            final String charset = Charset.isSupported("UTF8") ? "UTF8" : Charset
                .defaultCharset().name();

            out = new PrintStream(new FileOutputStream(output), true, charset);
        }

        /*
         * Calculate random input for the maximum size we will run on. Turns out random
         * number generator is slower than suffix array computation in most cases...
         */
        final Runtime rt = Runtime.getRuntime();

        logger.info("Algorithm: " + algorithm + ", alphabet: " + startAlphabet
            + "-" + (startAlphabet + rounds * increment)
            + ", input size: " + inputSize
            + ", extraCells: " + extraCells + ", seed: " + randomSeed);

        logger.info("Time: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
        logger.info("JVM name: " + JAVA_VM_NAME);
        logger.info("JVM version: " + JAVA_VM_VERSION);
        logger.info("JVM info: " + JAVA_VM_INFO);
        logger.info("JVM vendor: " + JAVA_VM_VENDOR);
        logger.info("OS arch: " + SystemUtils.OS_ARCH);
        logger.info("OS name: " + SystemUtils.OS_NAME);
        logger.info("OS version: " + SystemUtils.OS_VERSION);
        logger.info("JVM max memory: " + rt.maxMemory());

        logger.info("Allocating random input: " + inputSize + " bytes.");
        final int [] input = new int [inputSize + extraCells];

        out.println(String.format(Locale.US, "# %4s " + "%8s " + "%7s " + "%7s " + "%5s  "
            + "%s", "rnd", "alph.size", "time", "mem(MB)", "av.lcp", "status"));

        /*
         * Run the test. Warmup rounds have negative round numbers.
         */
        final ISuffixArrayBuilder builder = algorithm.getInstance();
        int size = startAlphabet;
        for (int round = -warmup; round < rounds; round++)
        {
            for (int sample = 0; sample < samples; sample++) {
                MemoryLogger.reset();

                final MinMax alphabet = new MinMax(1, size);
                SuffixArrayBuilderTestBase.fillRandom(rnd, input, inputSize, alphabet);

                // Run the test.
                final long startTime = System.currentTimeMillis();
                final long endTime;
                String status = "ok";
                double averageLCP = 0;
                try
                {
                    final int [] sa = builder.buildSuffixArray(input, 0, inputSize);
                    final int [] lcp = SuffixArrays.computeLCP(input, 0, inputSize, sa);
                    long prefixesLen = 0;
                    for (int i = 0; i < inputSize; i++)
                        prefixesLen += lcp[i];
                    averageLCP = prefixesLen / (double) inputSize;
                }
                catch (OutOfMemoryError t)
                {
                    status = "oom";
                }
                catch (Throwable t)
                {
                    status = "err";
                }
                finally
                {
                    endTime = System.currentTimeMillis();
                }
    
                // round, input size, suffix building time, mem used (MB), avg.lcp, status
                final String result = String.format(Locale.US, "%6d " + "%8d " + "%7.3f "
                    + "%7.3f " + "%5.2f  " + "%s", round, size,
                    (endTime - startTime) / 1000.0d, MemoryLogger.getMemoryUsed()
                        / (double) (1024 * 1024), averageLCP, status);
                out.println(result);
    
                if (round >= 0)
                {
                    size += increment;
                }
            }
        }

        IOUtils.closeQuietly(out);
        logger.info("Completed.");
    }

    /*
     * Console entry point.
     */
    public static void main(String [] args) throws Exception
    {
        final TimeOnAlphabetSize launcher = new TimeOnAlphabetSize();
        final CmdLineParser parser = new CmdLineParser(launcher);
        parser.setUsageWidth(80);

        try
        {
            parser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            PrintStream ps = System.out;
            ps.print("Usage: ");
            parser.printSingleLineUsage(ps);
            ps.println();
            parser.printUsage(ps);

            ps.println("\n" + e.getMessage());
            return;
        }

        launcher.run();
    }
}
