package org.jsuffixarrays;

/**
 * Simple mapper that maps input to values larger than zero.
 */
public class PositiveMapper implements IMapper
{
    private final int offset;

    public PositiveMapper(int [] input, int start, int length)
    {

        final MinMax minmax = Tools.minmax(input, start, length);
        offset = 1 - minmax.min;
        Tools.assertAlways(Integer.MAX_VALUE >= minmax.max + offset,
            "Alphabet is too big");
    }

    @Override
    public void map(int [] input, int start, int length)
    {
        for (int i = start, l = length; l > 0; l--, i++)
        {
            input[i] = input[i] + offset;
        }
    }

    @Override
    public void undo(int [] input, int start, int length)
    {
        for (int i = start, l = length; l > 0; l--, i++)
        {
            input[i] = input[i] - offset;
        }
    }
}
