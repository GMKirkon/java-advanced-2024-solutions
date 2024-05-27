package info.kgeorgiy.ja.konovalov.bank;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;


public class BankTests {
    /* Working solution 1, that uses Georgiy Korneev's BaseTester class
     from java-advanced course: https://www.kgeorgiy.info/courses/java-advanced/index.html
     
    public static void main(String... args) {
        BaseTester tester = new BaseTester();
        tester.add("bank", ActualBankTests.class);
    }
    
    static void test(BaseTester tester, String... args) {
        tester.run(args);
    }*/
    
    
    public static void main(String... args) {
        int total = 0;
        total += test(ActualBankTests.class);
        System.out.printf("Finished with %d errors", total);
        System.exit(total);
    }
    
    //solution base on open API from junit5
    //https://junit.org/junit5/docs/5.0.3/api/org/junit/platform/launcher/Launcher.html
    private static int test(final Class<?> test) {
        System.err.printf("Running %s%n", test);
        
        final SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        
        //based on s7vr answer on
        //https://stackoverflow.com/questions/41386402/discovering-tests-on-provided-classpath-via-launcherdiscoveryrequest
        final LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                                               .selectors(DiscoverySelectors.selectClass(test))
                                               .build();
        LauncherFactory.create().execute(request, summaryListener);
        final TestExecutionSummary summary = summaryListener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        return summary.getTestsFailedCount() != 0 ? 1 : 0;
    }
}
