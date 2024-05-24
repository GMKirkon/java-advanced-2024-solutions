package info.kgeorgiy.ja.konovalov.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static info.kgeorgiy.ja.konovalov.hello.Internal.addDatagramChannel;
import static info.kgeorgiy.ja.konovalov.hello.Internal.closeDatagramChannels;
import static info.kgeorgiy.ja.konovalov.hello.Internal.closeThread;
import static info.kgeorgiy.ja.konovalov.hello.Internal.closeWithExceptionConsumer;
import static info.kgeorgiy.ja.konovalov.hello.Internal.performSelectingOperation;
import static info.kgeorgiy.ja.konovalov.hello.Internal.suppressingConsumer;
import static info.kgeorgiy.ja.konovalov.hello.Internal.unwrapSuppressedException;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    
    private static final int DEFAULT_TIMOUT_IN_MILLISECONDS = 5;
    private static final int MAX_SAVED_RESPONSES = 41;
    final AtomicReference<Exception> totalException = new AtomicReference<>();
    final List<DatagramChannel> datagramChannels = new ArrayList<>();
    final Consumer<Exception> addSuppressedException = suppressingConsumer(totalException);
    Selector selector;
    Thread workingThread;
    
    private static class State extends AbstractNonblockingUDPState {
        final String formattingString;
        Queue<QueryAnswer> answers = new LinkedBlockingQueue<>(MAX_SAVED_RESPONSES);
        
        private State(String formattingString) {
            super(HANDLER_BUFFER_SIZE);
            this.formattingString = formattingString;
        }
    }
    
    /**
     * creates Server that would be closed after timeout, note that timeout is in int,
     * so server could be working upto ~63 YEARS
     *
     * @param timeoutInSeconds time until executing close()
     */
    public HelloUDPNonblockingServer(int timeoutInSeconds) {
        super(timeoutInSeconds);
    }
    
    /**
     * Creates Server without timeout
     */
    public HelloUDPNonblockingServer() {
        super();
    }
    
    @Override
    public void start(int threads, Map<Integer, String> ports) {
        startHelper(threads, ports);
        if (currentState == serverStates.DOING_NOTHING_EMPTY) {
            return;
        }
        
        try {
            selector = Selector.open();
            try {
                ports.forEach((port, formattingString) -> {
                    var address = new InetSocketAddress(port);
                    try {
                        final DatagramChannel channel = addDatagramChannel(datagramChannels);
                        channel.bind(address);
                        channel.register(selector, SelectionKey.OP_READ, new State(formattingString));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (RuntimeException e) {
                addSuppressedException.accept(e);
            }
            
            if (totalException.get() != null) {
                closeDatagramChannels(datagramChannels, addSuppressedException);
                unwrapSuppressedException(totalException, true);
            }
            
            workingThread = new Thread(() -> {
                try {
                    run(selector);
                } catch (Exception e) {
                    addSuppressedException.accept(e);
                }
            });
            
            unwrapSuppressedException(totalException, true);
            workingThread.start();
        } catch (IOException e) {
            addSuppressedException.accept(e);
        }
    }
    
    private void run(final Selector selector) throws IOException {
        performSelectingOperation(selector, addSuppressedException, (key, channel) -> {
            try {
                final var state = (State) key.attachment();
                final var address = state.actualReceiveFromChannel(channel);
                final var data = state.getResponseFromState(SERVER_CHARSET);
                queryHandlersPool.submit(() -> hardLongWorkingExtractingOperation(selector, key, state, data, address));
            } catch (IOException e) {
                addSuppressedException.accept(e);
            }
        }, (key, channel) -> {
            try {
                final var state = (State) key.attachment();
                var queryAnswer = state.answers.poll();
                if (queryAnswer == null) {
                    key.interestOps(SelectionKey.OP_READ);
                } else {
                    channel.send(ByteBuffer.wrap(queryAnswer.answer().getBytes(SERVER_CHARSET)), queryAnswer.address());
                }
            } catch (IOException e) {
                addSuppressedException.accept(e);
            }
        }, DEFAULT_TIMOUT_IN_MILLISECONDS, () -> {
        });
    }
    
    private static void hardLongWorkingExtractingOperation(Selector selector, SelectionKey key, State state, String data, SocketAddress address) {
        String transformedData = state.formattingString.replaceAll("\\$", data);
        QueryAnswer answer = new QueryAnswer(transformedData, address);
        state.answers.add(answer);
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        selector.wakeup();
    }
    
    @Override
    protected AbstractHelloUDPServer getServer() {
        return new HelloUDPNonblockingServer();
    }
    
    @Override
    protected AbstractHelloUDPServer getServer(int timeout) {
        return new HelloUDPNonblockingServer(timeout);
    }
    
    @Override
    protected void closeAllRunningThreads() {
        currentState = serverStates.CLOSING;
        
        closeThread(workingThread);
        
        closeWithExceptionConsumer(queryHandlersPool, addSuppressedException);
        closeWithExceptionConsumer(selector, addSuppressedException);
        closeDatagramChannels(datagramChannels, addSuppressedException);
    }
    
    /**
     * Gets the total accumulated exception for server
     * @return exception after server was closed
     */
    public Exception getTotalNonBlockingException() {
        if (currentState != serverStates.CLOSED) {
            return null;
        }
        return totalException.get();
    }
}
