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
        return new DensePositiveDecorator(
            new ExtraTrailingCellsDecorator(new Skew(), 3));
    }
}