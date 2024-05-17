package info.kgeorgiy.ja.konovalov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    static private final int DEFAULT_CLIENT_WAITING_FOR_RESPONSE_TIMOUT_TIME_IN_MILLISECONDS = 45;
    //holy cow legacy is really evil...
    static private final String regex = "([\\p{IsAlphabetic}-._]+), ([\\p{IsAlphabetic}-._()]+)(\\p{N}+)([-_]+)(\\p{N}+)$";
    static private final Pattern pattern = Pattern.compile(regex);
    static private final NumberFormat numberFormat = NumberFormat.getNumberInstance();
    
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
    
    private static String formServerRequest(String prefix, int numberOfThread, int numberOfQuery) {
        return String.format("%s%d_%d", prefix, numberOfThread, numberOfQuery);
    }
    
    /**
     * Creates InetSocketAddress from provided host and port
     *
     * @param host host for address
     * @param port port for address
     * @return created InetSocketAddress
     * @throws ImpossibleToGetAddressException in case its impossible to get corrent InetSocketAddress for provided host and port
     */
    private static InetSocketAddress getInetSocketAddress(String host, int port) throws ImpossibleToGetAddressException {
        try {
            return new InetSocketAddress(host, port);
        } catch (SecurityException | IllegalArgumentException e) {
            throw new ImpossibleToGetAddressException(host, port, e);
        }
    }
    
    private static String getStringFromPacket(DatagramPacket answerPacket) {
        return new String(
                answerPacket.getData(),
                answerPacket.getOffset(),
                answerPacket.getLength(),
                StandardCharsets.UTF_8
        );
    }
    
    private static void getResponseFromServer(DatagramPacket requestPacket, byte[] buffer, byte[] requestBytes, DatagramSocket socket, DatagramPacket answerPacket) {
        boolean receivedSomething = false;
        while (!receivedSomething) {
            try {
                requestPacket.setData(buffer, 0, requestBytes.length);
                System.arraycopy(requestBytes, 0, buffer, 0, requestBytes.length);
                socket.send(requestPacket);
                
                socket.receive(answerPacket);
                receivedSomething = true;
            } catch (IOException ignored) {
            }
        }
    }
    
    private static byte[] getBufferForMessagingWithServer(String prefix, int requests, int numberOfThread, DatagramSocket socket) throws SocketException {
        return new byte[Integer.max(
                socket.getReceiveBufferSize(),
                formServerRequest(prefix, numberOfThread, requests).getBytes(StandardCharsets.UTF_8).length
        )];
    }
    
    public static void main(String... args) {
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
        run(address, prefix, threads, requests);
    }
    
    private static void printUsage() {
        System.out.println("Usage: <ip/name> <port> <prefix> <threads> <requests>");
        System.out.println("threads and requests should be positive, ip should be correct");
    }
    
    
    private static void run(SocketAddress address, String prefix, int threads, int requests) {
        Internal.checkForPositive(threads, "could not sends requests in non positive number of threads", true);
        Internal.checkForPositive(requests, "could not sends non positive number of requests", true);
        
        AtomicReference<Exception> requestException = new AtomicReference<>();
        
        Consumer<Exception> addSuppressedException = (Exception e) -> {
            requestException.getAndUpdate(currentException -> {
                if (currentException == null) {
                    return e;
                }
                currentException.addSuppressed(e);
                return currentException;
            });
        };
        
        
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            IntStream.range(1, threads + 1).forEach(numberOfThread -> {
                executorService.submit(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.connect(address);
                        byte[] buffer = getBufferForMessagingWithServer(prefix, requests, numberOfThread, socket);
                        
                        IntStream.range(1, requests + 1).forEach(numberOfRequest -> {
                            doSingleRequest(prefix, numberOfThread, numberOfRequest, buffer, socket, addSuppressedException);
                        });
                    } catch (IOException e) {
                        addSuppressedException.accept(e);
                    }
                });
            });
        }
        
        Exception queryException = requestException.get();
        if (queryException != null) {
            throw new UncheckedClientQueryException(queryException);
        }
    }
    
    private static void doSingleRequest(String prefix, int numberOfThread, int numberOfRequest, byte[] buffer, DatagramSocket socket, Consumer<Exception> addSuppressedException) {
        try {
            String request = formServerRequest(prefix, numberOfThread, numberOfRequest);
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
            
            DatagramPacket requestPacket = new DatagramPacket(buffer, requestBytes.length);
            DatagramPacket answerPacket = new DatagramPacket(buffer, 0, buffer.length);
            
            socket.setSoTimeout(DEFAULT_CLIENT_WAITING_FOR_RESPONSE_TIMOUT_TIME_IN_MILLISECONDS);
            
            String answer = "";
            boolean gotCorrectResponse = false;
            while (!gotCorrectResponse) {
                getResponseFromServer(requestPacket, buffer, requestBytes, socket, answerPacket);
                
                answer = getStringFromPacket(answerPacket);
                if (checkAnswerForCorrectness(answer, numberOfThread, numberOfRequest)) {
                    gotCorrectResponse = true;
                }
            }
            System.out.printf("Request: %s%nAnswer: %s%n", request, answer);
        } catch (IOException e) {
            addSuppressedException.accept(e);
        }
    }
    
    private static boolean checkAnswerForCorrectness(String answer, int numberOfThread, int numberOfRequest) {
        Matcher matcher = pattern.matcher(answer);
        
        if (matcher.matches()) {
            String gotAsNumberOfThreads = matcher.group(3);
            String gotAsNumberOfRequest = matcher.group(5);
            
            try {
                int gotNumberOfRequests = numberFormat.parse(gotAsNumberOfRequest).intValue();
                int gotNumberOfThreads = numberFormat.parse(gotAsNumberOfThreads).intValue();
                
                return gotNumberOfRequests == numberOfRequest &&
                       gotNumberOfThreads == numberOfThread;
            } catch (ParseException ignored) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress address = getInetSocketAddress(host, port);
        run(address, prefix, threads, requests);
    }
}
