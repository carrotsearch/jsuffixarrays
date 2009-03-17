package org.jsuffixarrays;


/**
 * Tests for {@link Skew} with decorators allowing any symbols on the input.
 */
public class SkewWithDecoratorsTest extends SuffixArrayBuilderTestBase
{
    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance()
    {
        return new NonNegativeCompactingDecorator(
            new ExtraCellsZeroIndexDecorator(new Skew(), 3));
    }
}