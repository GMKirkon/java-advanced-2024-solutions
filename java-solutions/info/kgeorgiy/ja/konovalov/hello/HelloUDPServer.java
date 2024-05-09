package info.kgeorgiy.ja.konovalov.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements NewHelloServer {
    static private final Charset SERVER_CHARSET = StandardCharsets.UTF_8;
    static private final int HANDLER_BUFFER_SIZE = 1024;
    private final ConcurrentLinkedQueue<Exception> receivingExceptions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Exception> sendingExceptions = new ConcurrentLinkedQueue<>();
    private final Timer timer;
    private final List<PortListener> listeners = new ArrayList<>();
    private ExecutorService portListenersPool;
    private ExecutorService queryHandlersPool;
    private volatile serverStates currentState = serverStates.NOT_STARTED;
    
    /**
     * enum for server states, by contract server could only be run once
     */
    private enum serverStates {
        NOT_STARTED,
        WORKING,
        DOING_NOTHING_EMPTY,
        CLOSING
    }
    
    /**
     * TimerTask to close Server after some timeout
     */
    private final class SelfCloseTask extends TimerTask {
        @Override
        public void run() {
            close();
        }
    }
    
    /**
     * Class that received all queries from specified port, and sends the request to workers to create and send
     * the answer for the request
     */
    private final class PortListener {
        private final DatagramSocket socket;
        private final byte[] handlerBuffer;
        private final String formattingString;
        
        public PortListener(int port, String formattingString) throws UncheckedSocketException {
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException e) {
                throw new UncheckedSocketException(e);
            }
            
            handlerBuffer = new byte[HANDLER_BUFFER_SIZE];
            this.formattingString = formattingString;
        }
        
        public void listen() {
            while (currentState != serverStates.CLOSING && !Thread.currentThread().isInterrupted()) {
                DatagramPacket receivingPacket = new DatagramPacket(handlerBuffer, 0, HANDLER_BUFFER_SIZE);
                try {
                    socket.receive(receivingPacket);
                    String data = new String(
                            receivingPacket.getData(),
                            receivingPacket.getOffset(),
                            receivingPacket.getLength(),
                            SERVER_CHARSET
                    );
                    SocketAddress address = receivingPacket.getSocketAddress();
                    
                    queryHandlersPool.submit(() -> {
                        DatagramPacket sendingPacket = createSendingPacketFromReceived(data, address, formattingString);
                        try {
                            socket.send(sendingPacket);
                        } catch (PortUnreachableException e) {
                            sendingExceptions.add(e);
                            waitWhilePortIsUnreachable();
                        } catch (SecurityException | IOException e) {
                            sendingExceptions.add(e);
                        }
                    });
                } catch (PortUnreachableException e) {
                    receivingExceptions.add(e);
                    waitWhilePortIsUnreachable();
                } catch (SocketException caught) {
                    // was interrupted during receiving, so just exit the loop
                    break;
                } catch (IOException e) {
                    receivingExceptions.add(e);
                }
            }
        }
        
        private static DatagramPacket createSendingPacketFromReceived(String data, SocketAddress address, String formattingString) {
            String answer = formattingString.replaceAll("\\$", data);
            byte[] bytesOfAnswer = answer.getBytes(SERVER_CHARSET);
            DatagramPacket sendingPacket = new DatagramPacket(bytesOfAnswer, 0, bytesOfAnswer.length);
            
            sendingPacket.setSocketAddress(address);
            return sendingPacket;
        }
        
        private static void waitWhilePortIsUnreachable() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
    
    /**
     * creates Server that would be closed after timeout, note that timeout is in int,
     * so server could be working upto ~63 YEARS
     *
     * @param timeoutInSeconds time until executing close()
     */
    public HelloUDPServer(int timeoutInSeconds) {
        timer = new Timer();
        timer.schedule(new SelfCloseTask(), timeoutInSeconds * 1000L);
    }
    
    
    /**
     * Creates Server without timeout
     */
    public HelloUDPServer() {
        timer = null;
    }
    
    public static void main(String... args) {
        if (args == null) {
            System.out.println("Provided args should not be null");
            printUsage();
            return;
        }
        
        if (args.length != 2) {
            printUsage();
        }
        
        Integer port = Internal.parsePositiveInteger(args[0], "port ");
        Integer threads = Internal.parsePositiveInteger(args[1], "number of threads ");
        if (port == null || threads == null) {
            return;
        }
        
        // yes, try without resources since wanna run server, but not close it just after the start
        var server = new HelloUDPServer();
        server.start(port, threads);
    }
    
    private static void printUsage() {
        System.out.println("Usage: <port> <number of handlers>");
    }
    
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        switch (currentState) {
            case WORKING, DOING_NOTHING_EMPTY -> throw new IllegalStateException("Server is already running");
            case CLOSING -> throw new IllegalStateException("Server already finished working and could not be restarted");
        }
        
        
        if (ports.isEmpty()) {
            currentState = serverStates.DOING_NOTHING_EMPTY;
            return;
        }
        
        currentState = serverStates.WORKING;
        createListeners(ports);
        
        portListenersPool = Executors.newFixedThreadPool(ports.size());
        queryHandlersPool = Executors.newFixedThreadPool(threads);
        
        for (var listener : listeners) {
            portListenersPool.submit(listener::listen);
        }
    }
    
    /**
     * Fills list of listeners with created {@link PortListener}
     * @param ports port no to response format mapping.
     * @throws UncheckedSocketException if could not create listener for some port
     */
    private void createListeners(Map<Integer, String> ports) throws UncheckedSocketException {
        final UncheckedSocketException[] startingException = {null};
        
        ports.entrySet().stream().peek(entry -> {
            try {
                listeners.add(new PortListener(entry.getKey(), entry.getValue()));
            } catch (UncheckedSocketException e) {
                if (startingException[0] == null) {
                    startingException[0] = e;
                } else {
                    startingException[0].addSuppressed(e);
                }
            }
        }).toList();
        
        if (startingException[0] != null) {
            currentState = serverStates.CLOSING;
            listeners.stream().peek(x -> x.socket.close()).toList();
            throw startingException[0];
        }
    }
    
    
    private void closeIgnoringAllRunningThreads() {
        currentState = serverStates.CLOSING;
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
    
    private void closeAllRunningThreads() {
        listeners.stream().peek(x -> x.socket.close()).toList();
        listeners.clear();
        queryHandlersPool.close();
        portListenersPool.close();
    }
    
    @Override
    public void close() {
        if (currentState == serverStates.DOING_NOTHING_EMPTY) {
            closeIgnoringAllRunningThreads();
            return;
        }
        
        closeIgnoringAllRunningThreads();
        closeAllRunningThreads();
        currentState = serverStates.NOT_STARTED;
    }
    
    /**
     * Returns list of exceptions happened during receiving data, if server is still running returns null
     *
     * @return List of receiving exceptions that happened during last server session
     */
    public List<Exception> getReceivingExceptions() {
        return getExceptions(receivingExceptions);
    }
    
    private List<Exception> getExceptions(Collection<Exception> exceptions) {
        if (currentState != serverStates.CLOSING) {
            return null;
        }
        return exceptions.stream().toList();
    }
    
    /**
     * Returns list of exceptions happened during sending data, if server is still running returns null
     *
     * @return List of sending exceptions that happened during last server session
     */
    public List<Exception> getSendingExceptions() {
        return getExceptions(sendingExceptions);
    }
}
