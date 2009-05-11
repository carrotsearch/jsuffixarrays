package org.jsuffixarrays;

import static org.apache.commons.lang.SystemUtils.JAVA_VM_INFO;
import static org.apache.commons.lang.SystemUtils.JAVA_VM_NAME;
import static org.apache.commons.lang.SystemUtils.JAVA_VM_VENDOR;
import static org.apache.commons.lang.SystemUtils.JAVA_VM_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measure time taken to build a suffix array on input read from a file.
 */
public class TimeOnFile
{
    private final Logger logger = LoggerFactory.getLogger(TimeOnRandomInput.class);

    @Option(aliases =
    {
        "--rounds"
    }, metaVar = "int", name = "-r", required = false, usage = "Number of rounds")
    public int rounds = 100;

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
        "--output-file"
    }, metaVar = "file", name = "-o", required = false, usage = "Output file (if not given, stdout is used)")
    public File output;

    @Argument(index = 0, required = true, usage = "Algorithm to test.")
    public Algorithm algorithm;

    @Argument(index = 1, required = true, usage = "Input data file.")
    public File inputFile;

    /*
     * Run the performance test.
     */
    private void run() throws IOException
    {
        Tools.assertAlways(extraCells >= 0, "extra cells must be >= 0");

        PrintStream out = System.out;
        if (output != null)
        {
            final String charset = Charset.isSupported("UTF8") ? "UTF8" : Charset
                .defaultCharset().name();

            out = new PrintStream(new FileOutputStream(output), true, charset);
        }

        final Runtime rt = Runtime.getRuntime();

        logger.info("Algorithm: " + algorithm + ", file: " + inputFile.getName()
            + ", extraCells: " + extraCells);

        logger.info("JVM name: " + JAVA_VM_NAME);
        logger.info("JVM version: " + JAVA_VM_VERSION);
        logger.info("JVM info: " + JAVA_VM_INFO);
        logger.info("JVM vendor: " + JAVA_VM_VENDOR);
        logger.info("JVM max memory: " + rt.maxMemory());

        final int size = (int) inputFile.length();
        final int [] input = new int [size + extraCells];

        final FileInputStream fis = new FileInputStream(inputFile);
        try
        {
            final byte [] buffer = new byte [1024 * 16];
            int len;
            int pos = 0;
            while ((len = fis.read(buffer)) >= 0)
            {
                for (int i = 0; i < len; i++, pos++)
                {
                    input[pos] = buffer[i];
                    // all original algorithms in C read bytes as unsigned chars
                    // we simulate it here to have the same ranks of symbols in input
                    if (input[pos] < 0)
                    {
                        input[pos] += 256;
                    }
                }
            }
        }
        finally
        {
            fis.close();
        }

        final int start = 0;
        final IMapper mapper = algorithm.getMapper(input, start, size);
        if (mapper != null)
        {
            mapper.map(input, start, size);
        }

        out.println(String.format(Locale.US, "%4s " + "%7s " + "%7s " + "%7s " + "%5s  "
            + "%s", "rnd", "size", "time", "mem(MB)", "av.lcp", "status"));

        /*
         * Run the test. Warmup rounds have negative round numbers.
         */
        logger.info("Running the test.");
        final ISuffixArrayBuilder builder = algorithm.getInstance();
        for (int round = -warmup; round < rounds; round++)
        {
            MemoryLogger.reset();

            // Run the test.
            final long startTime = System.currentTimeMillis();
            final long endTime;
            String status = "ok";
            double averageLCP = 0;
            try
            {
                final int [] sa = builder.buildSuffixArray(input, 0, size);
                final int [] lcp = SuffixArrays.computeLCP(input, 0, size, sa);
                long prefixesLen = 0;
                for (int i = 0; i < size; i++)
                {
                    prefixesLen += lcp[i];
                }
                averageLCP = prefixesLen / (double) size;
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

            // round, input size, suffix building time, mem used (MB), avg.lcp,
            // status
            final String result = String.format(Locale.US, "%4d " + "%7d " + "%7.3f "
                + "%7.3f " + "%5.2f  " + "%s", round, size,
                (endTime - startTime) / 1000.0d, MemoryLogger.getMemoryUsed()
                    / (double) (1024 * 1024), averageLCP, status);
            out.println(result);

        }

        IOUtils.closeQuietly(out);
        logger.info("Completed.");
    }

    /*
     * Console entry point.
     */
    public static void main(String [] args) throws Exception
    {
        final TimeOnFile launcher = new TimeOnFile();
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
