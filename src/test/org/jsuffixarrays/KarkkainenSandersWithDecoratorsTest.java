package org.jsuffixarrays;


/**
 * Tests for {@link KarkkainenSanders} with decorators allowing any symbols on the input.
 */
public class KarkkainenSandersWithDecoratorsTest extends SuffixArrayBuilderTestBase
{
    /*
     * 
     */
    @Override
    protected ISuffixArrayBuilder getInstance()
    {
        return new NonNegativeCompactingDecorator(
            new ExtraCellsZeroIndexDecorator(new KarkkainenSanders(), 3));
    }
}