package socketOfServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/* nio를 사용
connect() -> accpet() 이후 SocketChannel object의 read, write method 호출을통해 데이터 통신
*  Buffer로 읽고 쓴다.*/
public class BSimpleClient {
    public static void main(String[] args) {
        SocketChannel socketChannel = null;
        try{
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            System.out.println("Connection Request!!");
            socketChannel.connect(new InetSocketAddress("localhost", 5001));
            System.out.println(" Connection Success");

            ByteBuffer byteBuffer = null;
            Charset charset = Charset.forName("UTF-8");

            byteBuffer = charset.encode("Hello World");
            socketChannel.write(byteBuffer);
            System.out.println("Success sending data");

            byteBuffer = ByteBuffer.allocate(100);
            int byteCount = socketChannel.read(byteBuffer);
            byteBuffer.flip(); //읽기 모드를 쓰기 모드로 바꾸는 것
            String message = charset.decode(byteBuffer).toString();
            System.out.println("Success receiving DATA!!!");


        }catch (Exception e){}

        if(socketChannel.isOpen()){
            try{socketChannel.close();}catch (IOException e1) {}
        }
    }
}
