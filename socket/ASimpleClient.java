package socketOfServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/* nio를 사용한 단순 연결
localhost 5001 port 에 connection 요청 */

public class ASimpleClient {
    public static void main(String[] args) {

        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            System.out.println("[ Connection request!!!] ");
            socketChannel.connect(new InetSocketAddress("localhost", 5001));
            System.out.println("[ Connection Success!!] " );
        }catch(Exception e){}
        if(socketChannel.isOpen()) {
            try {
                socketChannel.close();

            }catch (IOException e1) {
            }
        }
    }
}
