package org.jsuffixarrays;

/**
 * A decorator around {@link ISuffixArrayBuilder} that accepts any input symbols and maps
 * it to non-negative, compact alphabet. Relative symbols order is preserved (changes are
 * limited to a constant shift and compaction of symbols). The input is remapped in-place,
 * but additional space is required for the mapping.
 */
public final class NonNegativeCompactingDecorator implements ISuffixArrayBuilder
{
    private final ISuffixArrayBuilder delegate;

    /*
     * 
     */
    public NonNegativeCompactingDecorator(ISuffixArrayBuilder delegate)
    {
        this.delegate = delegate;
    }

    /*
     * 
     */
    @Override
    public int [] buildSuffixArray(int [] input, final int start, final int length)
    {
        final MinMax minmax = Tools.minmax(input, start, length);

        final IMapper mapper;
        if (minmax.range() > 0x10000)
        {
            throw new RuntimeException("Large symbol space not implemented yet.");
        }
        else
        {
            mapper = new DensePositiveMapper(input, start, length);
        }

        mapper.map(input, start, length);
        try
        {
            return delegate.buildSuffixArray(input, start, length);
        }
        finally
        {
            mapper.undo(input, start, length);
        }
    }
}
