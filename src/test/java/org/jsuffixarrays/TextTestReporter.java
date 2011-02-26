package org.jsuffixarrays;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class TextTestReporter extends TestListenerAdapter
{
    @Override
    public void onTestFailure(ITestResult tr)
    {
        super.onTestFailure(tr);
        report("FAILURE", tr);
    }

    @Override
    public void onTestSkipped(ITestResult tr)
    {
        super.onTestSkipped(tr);
        report("SKIPPED", tr);
    }
    
    @Override
    public void onTestSuccess(ITestResult tr)
    {
        super.onTestSuccess(tr);
        report("OK", tr);
    }
    
    private void report(String status, ITestResult tr)
    {
        synchronized (this)
        {
            System.out.println(String.format(
                "[%s] %.2f |> %s",
                status,
                (tr.getEndMillis() - tr.getStartMillis()) / 1000.0,
                getTestMethodName(tr.getMethod())
                ));
        }
    }

    private String getTestMethodName(ITestNGMethod method)
    {
        return 
            method.getTestClass().getRealClass().getSimpleName() + "#" +
            method.getMethod().getName();
    }

    @Override
    public void onFinish(ITestContext ctx)
    {
        super.onFinish(ctx);

        System.out.println("----");
        System.out.println(
            String.format("Passed: %d  Failed: %d  Skipped: %d", 
                ctx.getPassedTests().size(),
                ctx.getFailedTests().size(),
                ctx.getSkippedTests().size()));
    }
}
