package info.kgeorgiy.ja.konovalov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractHelloUDPClient implements HelloClient {
    
    static protected final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    static protected final int DEFAULT_CLIENT_WAITING_FOR_RESPONSE_TIMOUT_TIME_IN_MILLISECONDS = 10;
    
    //holy cow legacy is really evil...
    static protected final String regex = "([\\p{IsAlphabetic}-._]+), ([\\p{IsAlphabetic}-._()]+)(\\p{N}+)([-_]+)(\\p{N}+)$";
    static protected final Pattern pattern = Pattern.compile(regex);
    
    private final static class ImpossibleToGetAddressException extends RuntimeException {
        ImpossibleToGetAddressException(String host, int port, RuntimeException e) {
            super(String.format(
                    "Could not create address for provided host: %s and port: %d%n, with error: %s",
                    host,
                    port,
                    e.getMessage()
            ));
        }
    }
    
    private static InetSocketAddress getInetSocketAddress(String host, int port) throws ImpossibleToGetAddressException {
        try {
            return new InetSocketAddress(host, port);
        } catch (SecurityException | IllegalArgumentException e) {
            throw new ImpossibleToGetAddressException(host, port, e);
        }
    }
    
    protected static void printUsage() {
        System.out.println("Usage: <ip/name> <port> <prefix> <threads> <requests>");
        System.out.println("threads and requests should be positive, ip should be correct");
    }
    
    protected static boolean checkAnswerForCorrectness(String answer, int numberOfThread, int numberOfRequest) {
        Matcher matcher = pattern.matcher(answer);
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        
        if (matcher.matches()) {
            String gotAsNumberOfThreads = matcher.group(3);
            String gotAsNumberOfRequest = matcher.group(5);
            
            try {
                int gotNumberOfRequests = numberFormat.parse(gotAsNumberOfRequest).intValue();
                int gotNumberOfThread = numberFormat.parse(gotAsNumberOfThreads).intValue();
                
                /*System.err.printf(
                        "Expected digits: %d %d, got digits: %d %d%n",
                        numberOfRequest,
                        numberOfThread,
                        gotNumberOfRequests,
                        gotNumberOfThread
                );*/
                
                return gotNumberOfRequests == numberOfRequest &&
                       gotNumberOfThread == numberOfThread;
            } catch (ParseException ignored) {
                //                System.err.printf("COULD NOT MATCH: %s%n", answer);
                return false;
            }
        } else {
            //            System.err.printf("DOES NOT MATCH: %s %n", answer);
            return false;
        }
    }
    
    protected static byte[] getBufferForMessagingWithServer(String prefix, int requests, int numberOfThread, DatagramSocket socket) throws SocketException {
        return new byte[getBufferSizeForMessagingWithServer(prefix, requests, numberOfThread, socket)];
    }
    
    protected static int getBufferSizeForMessagingWithServer(String prefix, int requests, int numberOfThread, DatagramSocket socket) throws SocketException {
        return Integer.max(
                socket.getReceiveBufferSize(),
                formServerRequest(prefix, numberOfThread, requests).getBytes(StandardCharsets.UTF_8).length
        );
    }
    
    protected static String formServerRequest(String prefix, int numberOfThread, int numberOfQuery) {
        return String.format("%s%d_%d", prefix, numberOfThread, numberOfQuery);
    }
    
    protected static void printResponse(String request, String answer) {
        System.out.printf("Request: %s%nAnswer: %s%n", request, answer);
    }
    
    abstract protected AbstractHelloUDPClient getClient();
    
    protected void mainImpl(String... args) {
        Objects.requireNonNull(
                args,
                "first of all main is method to access HelloUDPClient from outside java, second, do not provide null as arguments"
        );
        if (args.length != 5) {
            printUsage();
            return;
        }
        
        Integer port = Internal.parsePositiveInteger(args[1], "port");
        String prefix = args[2];
        Integer threads = Internal.parsePositiveInteger(args[3], "threads");
        Integer requests = Internal.parsePositiveInteger(args[4], "requests");
        
        if (port == null) {
            printUsage();
            return;
        }
        
        SocketAddress address = InetSocketAddress.createUnresolved(args[0], port);
        
        if (threads == null || requests == null) {
            printUsage();
            return;
        }
        var client = getClient();
        client.run(address, prefix, threads, requests);
    }
    
    abstract protected void run(SocketAddress address, String prefix, int threads, int requests);
    
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress address = getInetSocketAddress(host, port);
        run(address, prefix, threads, requests);
    }
    
    protected void checkThreadsRequests(int threads, int requests) {
        Internal.checkForPositive(threads, "could not sends requests in non positive number of threads", true);
        Internal.checkForPositive(requests, "could not sends non positive number of requests", true);
    }
}
