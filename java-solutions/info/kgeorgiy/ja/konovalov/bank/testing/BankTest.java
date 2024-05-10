package info.kgeorgiy.ja.konovalov.bank.testing;

import info.kgeorgiy.java.advanced.base.BaseTest;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloClientTest;
import info.kgeorgiy.java.advanced.hello.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankTest extends BaseTest {
    private static final int PORT = 28888;
    public static final int SOCKET_FREE_TIME = 500;
    
    public BankTest() {
    }
    
    
    protected Callable<int[]> server(
            final String prefix,
            final int threads,
            final double p,
            final DatagramSocket socket
    ) {
        return Util.server(prefix, threads, p, socket);
    }
}
