package org.jsuffixarrays;

public interface IVisitor
{
    /**
     * Invoked before node is descended into.
     * 
     * @return Returning <code>false</code> omits the subtree of node. {@link #post(int)}
     *         is not invoked for this state if skipped.
     */
    public boolean pre(int nodeStart, int nodeLength);

    /**
     * Invoked after node is fully traversed.
     */
    public void post(int nodeStart, int nodeLength);

    /**
     * Invoked when an edge is visited.
     * 
     * @return Returning <code>false</code> skips the traversal of <code>toState</code>.
     */
    public boolean edge(int fromNodeStart, int fromNodeLength, int toNodeStart,
        int toNodeLength);
}
