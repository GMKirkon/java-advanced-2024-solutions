package info.kgeorgiy.ja.konovalov.hello;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.konovalov.hello.Internal.closeDatagramChannels;
import static info.kgeorgiy.ja.konovalov.hello.Internal.addDatagramChannel;
import static info.kgeorgiy.ja.konovalov.hello.Internal.suppressingConsumer;
import static info.kgeorgiy.ja.konovalov.hello.Internal.unwrapSuppressedException;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
    
    private static class State extends AbstractNonblockingUDPState {
        final int numberOfThread;
        final int numberOfRequests;
        final String prefix;
        int numberOfRequest;
        
        QueryAnswer queryAnswer;
        
        private State(String prefix, int numberOfThread, int requests, DatagramSocket socket, SocketAddress address) throws SocketException {
            super(getBufferSizeForMessagingWithServer(
                    prefix,
                    requests,
                    numberOfThread,
                    socket
            ));
            this.prefix = prefix;
            this.numberOfThread = numberOfThread;
            this.numberOfRequests = requests;
            this.queryAnswer = new QueryAnswer(formServerRequest(prefix, numberOfThread, numberOfRequest + 1), address);
        }
    }
    
    @Override
    protected AbstractHelloUDPClient getClient() {
        return new HelloUDPNonblockingClient();
    }
    
    @Override
    protected void run(SocketAddress address, String prefix, int threads, int requests) {
        checkThreadsRequests(threads, requests);
        
        final Exception[] requestException = {null};
        
        Consumer<Exception> addSuppressedException = suppressingConsumer(requestException);
        try (Selector selector = Selector.open()) {
            List<DatagramChannel> datagramChannels = new ArrayList<>();
            IntStream.range(1, threads + 1).forEach(numberOfThread -> {
                try {
                    final DatagramChannel channel = addDatagramChannel(datagramChannels);
                    channel.connect(address);
                    channel.register(selector, SelectionKey.OP_WRITE, new State(prefix, numberOfThread, requests, channel.socket(), address));
                } catch (IOException e) {
                    addSuppressedException.accept(e);
                }
            });
            
            if (requestException[0] != null) {
                closeDatagramChannels(datagramChannels, addSuppressedException);
                unwrapSuppressedException(requestException, true);
            }
            
            run(selector, addSuppressedException);
        } catch (IOException e) {
            addSuppressedException.accept(e);
        }
    }
    
    private void run(Selector selector, Consumer<Exception> addSuppressedException) throws IOException {
        Internal.performSelectingOperation(selector, addSuppressedException, (key, channel) -> {
            final State state = (State) key.attachment();
            try {
                final var address = state.actualReceiveFromChannel(channel);
                final var data = state.getResponseFromState(DEFAULT_CHARSET);
                if (checkAnswerForCorrectness(data, state.numberOfThread, state.numberOfRequest + 1)) {
                    printResponse(state.queryAnswer.answer(), data);
                    state.queryAnswer = null;
                    if (++state.numberOfRequest == state.numberOfRequests) {
                        channel.close();
                        return;
                    }
                }
                state.queryAnswer = new QueryAnswer(formServerRequest(state.prefix, state.numberOfThread, state.numberOfRequest + 1), address);
                key.interestOps(SelectionKey.OP_WRITE);
            } catch (IOException e) {
                addSuppressedException.accept(e);
            }
        }, (key, channel) -> {
            final State state = (State)key.attachment();
            try {
                var queryAnswer = state.queryAnswer;
                if (queryAnswer == null) {
                    key.interestOps(SelectionKey.OP_READ);
                } else {
                    System.out.println(queryAnswer.answer());
                    channel.send(ByteBuffer.wrap(queryAnswer.answer().getBytes(DEFAULT_CHARSET)), queryAnswer.address());
                    key.interestOps(SelectionKey.OP_READ);
                }
            } catch (IOException e) {
                addSuppressedException.accept(e);
            }

        }, DEFAULT_CLIENT_WAITING_FOR_RESPONSE_TIMOUT_TIME_IN_MILLISECONDS, () -> {
            selector.keys().forEach(x -> x.interestOps(SelectionKey.OP_WRITE));
        });
    }
}
