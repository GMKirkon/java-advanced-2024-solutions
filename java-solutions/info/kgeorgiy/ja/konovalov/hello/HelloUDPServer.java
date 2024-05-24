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

public class HelloUDPServer extends AbstractHelloUDPServer {
    private final List<PortListener> listeners = new ArrayList<>();
    private ExecutorService portListenersPool;
    
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
        super(timeoutInSeconds);
    }
    
    
    /**
     * Creates Server without timeout
     */
    public HelloUDPServer() {
        super();
    }
    
    public static void main(String... args) {
        try(var server = new HelloUDPServer()) {
            server.mainImpl(args);
        }
    }
    
    @Override
    protected AbstractHelloUDPServer getServer() {
        return new HelloUDPServer();
    }
    
    @Override
    protected AbstractHelloUDPServer getServer(int timeout) {
        return new HelloUDPServer(timeout);
    }
    
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        startHelper(threads, ports);
        if (currentState == serverStates.DOING_NOTHING_EMPTY) {
            return;
        }
        
        createListeners(ports);
        portListenersPool = Executors.newFixedThreadPool(ports.size());
        
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
        
        ports.forEach((key, value) -> {
            try {
                listeners.add(new PortListener(key, value));
            } catch (UncheckedSocketException e) {
                if (startingException[0] == null) {
                    startingException[0] = e;
                } else {
                    startingException[0].addSuppressed(e);
                }
            }
        });
        
        if (startingException[0] != null) {
            currentState = serverStates.CLOSING;
            listeners.forEach(x -> x.socket.close());
            throw startingException[0];
        }
    }
    
    
    @Override
    protected void closeAllRunningThreads() {
        listeners.forEach(x -> x.socket.close());
        listeners.clear();
        queryHandlersPool.close();
        portListenersPool.close();
    }
}
