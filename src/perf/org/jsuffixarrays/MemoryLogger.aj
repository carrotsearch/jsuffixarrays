package org.jsuffixarrays;

/**
 * This aspect logs the memory state at entry and exit from methods
 * declared in inheriting aspects. The aspect is a singleton, so is generally
 * not very useful for multi-threaded applications or whenever more than one instance
 * of a given instrumented class is used.
 */
abstract aspect MemoryLogger issingleton()
{
    /**
     * Declare classes to track memory from.
     */
    abstract pointcut tracedClasses();

    /**
     * Override and remove methods for which logging should not occur.
     */
    pointcut excludedMethods() : !execution(* *(..));

    /**
     * Declare methods to track memory in.
     */
    final pointcut tracedMethods() : 
        tracedClasses() && execution(* *(..)) && !excludedMethods();

    /**
     * 
     */
    before () : tracedClasses() && tracedMethods()
    {
        update();
    }

    /**
     * 
     */
    after () returning : tracedMethods()
    {
        update();
    }

    /**
     * Starting amount of memory from which we will begin measurements.
     * 
     * @see #reset();
     */
    private static volatile long startingPoint;
    
    /*
     * 
     */
    private static volatile long maxMemoryUsed;

    /*
     * 
     */
    private static void update()
    {
        final long now = getUsedMemory();
        if (maxMemoryUsed < now)
        {
            maxMemoryUsed = now;
        }
    }

    /*
     * 
     */
    private static long getUsedMemory()
    {
        final Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    /**
     * Reset memory status, preparing for measurement.
     */
    public static void reset()
    {
        // Try to clean the GC. Will not work for parallel GC, but we did our best.
        for (int i = 0; i < 5; i++)
        {
            System.gc();
        }
        
        startingPoint = getUsedMemory();
        maxMemoryUsed = 0;
    }

    /**
     * Returns the absolute memory peak recorded so far. 
     */
    public static long getMemoryPeak()
    {
        return maxMemoryUsed;
    }

    /**
     * Returns the maximum memory difference from the last time 
     * since {@link #reset()} was called. This value may be negative
     * with certain GCs and scenarios. 
     */
    public static long getMemoryUsed()
    {
        return maxMemoryUsed - startingPoint;
    }
}

/**
 * Track memory in {@link Skew} algorithm.
 */
aspect SkewMemLogger extends MemoryLogger
{
    @Override
    pointcut tracedClasses() : within(Skew);
    @Override
    pointcut excludedMethods(): execution(* leq*(..));
}

/**
 * Track memory in {@link NaiveSort2} algorithm.
 */
aspect NaiveSort2MemLogger extends MemoryLogger
{
    @Override
    pointcut tracedClasses() : within(NaiveSort2);
    @Override
    pointcut excludedMethods(): execution(* compare(..));
}

/**
 * Track memory in {@link DivSufSort} algorithm.
 */
aspect DivSufSortMemLogger extends MemoryLogger
{
    /* 3 methods using stack are excluded, because they use max. 64 * 4 bytes of extra memory for stack */
    //TODO: somehow exclude TRBudget class
    //within(DivSufSort && !DivSufSort.TRBudget) doesnt work);
    @Override
    pointcut tracedClasses(): within(DivSufSort);
    @Override
    pointcut excludedMethods(): execution(private * *(..)) ;
}

/**
 * Track memory in {@link NaiveSort2} algorithm.
 */
aspect QSUfSortMemLogger extends MemoryLogger
{
    @Override
    pointcut tracedClasses() : within(QSufSort);
    @Override
    pointcut excludedMethods(): execution(private * *(..));
}

