package org.jsuffixarrays;

/**
 * Symbol mappers (reversible int-coding).
 */
interface ISymbolMapper
{
    void map(int [] input, int start, int length);
    void undo(int [] input, int start, int length);
}
