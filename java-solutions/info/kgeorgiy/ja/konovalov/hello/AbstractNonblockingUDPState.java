package info.kgeorgiy.ja.konovalov.hello;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public abstract class AbstractNonblockingUDPState {
    final ByteBuffer handlerBuffer;
    
    AbstractNonblockingUDPState(int size) {
        handlerBuffer = ByteBuffer.allocate(size);
    }
    
    SocketAddress actualReceiveFromChannel(DatagramChannel channel) throws IOException {
        handlerBuffer.clear();
        return channel.receive(handlerBuffer);
    }
    
    String getResponseFromState(Charset charset) {
        handlerBuffer.flip();
        return charset.decode(handlerBuffer).toString();
    }
    
}
