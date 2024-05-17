package info.kgeorgiy.ja.konovalov.bank;

//import info.kgeorgiy.java.advanced.base.BaseTest;
//import info.kgeorgiy.java.advanced.base.BaseTester;

import info.kgeorgiy.java.advanced.base.BaseTester;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;


import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

public class BankTests {
    /* Working solution 1
    public static void main(String... args) {
        BaseTester tester = new BaseTester();
        tester.add("accounts", AccountTests.class);
        tester.add("bank", ActualBankTests.class);
        tester.add("persons", PersonsTests.class);
        
        test(tester, "accounts", "accounts");
        test(tester, "bank", "accounts");
        test(tester, "persons", "accounts");
    }
    
    static void test(BaseTester tester, String... args) {
        tester.run(args);
    }*/
    
    
    public static void main(String... args) {
        BaseTester tester = new BaseTester();
        int total = 0;
        total += test(AccountTests.class);
        total += test(PersonsTests.class);
        total += test(ActualBankTests.class);
        System.out.printf("Finished with %d errors", total);
        System.exit(total);
    }
    
    private static int test(final Class<?> test) {
        System.err.printf("Running %s%n", test);
        
        final SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                                                                                .selectors(DiscoverySelectors.selectClass(test))
                                                                                .build();
        LauncherFactory.create().execute(request, summaryListener);
        final TestExecutionSummary summary = summaryListener.getSummary();
        if (summary.getTestsFailedCount() == 0) {
            return 0;
        } else {
            return 1;
        }
    }
}