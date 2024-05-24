package info.kgeorgiy.ja.konovalov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.konovalov.hello.Internal.suppressingConsumer;

public class HelloUDPClient extends AbstractHelloUDPClient {
    
    private static String getStringFromPacket(DatagramPacket answerPacket) {
        return new String(
                answerPacket.getData(),
                answerPacket.getOffset(),
                answerPacket.getLength(),
                StandardCharsets.UTF_8
        );
    }
    
    private static void getResponseFromServer(DatagramPacket requestPacket, byte[] buffer, byte[] requestBytes, DatagramSocket socket, DatagramPacket answerPacket) throws IOException {
        boolean receivedSomething = false;
        while (!receivedSomething) {
            try {
                requestPacket.setData(buffer, 0, requestBytes.length);
                System.arraycopy(requestBytes, 0, buffer, 0, requestBytes.length);
                socket.send(requestPacket);
                
                socket.receive(answerPacket);
                receivedSomething = true;
            } catch (SocketTimeoutException ignored) {
                System.out.println("TIMEOUT: " + ignored.getMessage());
            }
        }
    }
    
    public static void main(String... args) {
        var client = new HelloUDPClient();
        client.mainImpl(args);
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
                } else {
                    System.err.printf(
                            "NOT CORRECT caught in thread %d in query %d : %s%n",
                            numberOfThread,
                            numberOfRequest,
                            answer
                    );
                }
            }
            printResponse(request, answer);
        } catch (Exception e) {
            addSuppressedException.accept(e);
        }
    }
    
    @Override
    protected final void run(SocketAddress address, String prefix, int threads, int requests) {
        checkThreadsRequests(threads, requests);
        AtomicReference<Exception> requestException = new AtomicReference<>();
        Consumer<Exception> addSuppressedException = suppressingConsumer(requestException);
        
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            IntStream.range(1, threads + 1).forEach(numberOfThread -> {
                executorService.submit(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.connect(address);
                        byte[] buffer = getBufferForMessagingWithServer(prefix, requests, numberOfThread, socket);
                        
                        IntStream.range(1, requests + 1).forEach(numberOfRequest -> {
                            doSingleRequest(
                                    prefix,
                                    numberOfThread,
                                    numberOfRequest,
                                    buffer,
                                    socket,
                                    addSuppressedException
                            );
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
    
    @Override
    protected AbstractHelloUDPClient getClient() {
        return new HelloUDPClient();
    }
}
