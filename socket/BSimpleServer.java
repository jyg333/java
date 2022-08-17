package socketOfServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class BSimpleServer {

    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = null;

        try{
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.bind(new InetSocketAddress(5001));
            while(true) {
                System.out.println(" Waiting Connection!!");
                SocketChannel socketChannel = serverSocketChannel.accept();
                InetSocketAddress isa = (InetSocketAddress) socketChannel.getRemoteAddress();
                System.out.println(" Accept Connection :" + isa.getHostName());

                ByteBuffer byteBuffer = null;
                Charset charset = Charset.forName("UTF-8");

                byteBuffer = ByteBuffer.allocate(100);
                int byteCount = socketChannel.read(byteBuffer);
                byteBuffer.flip();
                String message = charset.decode(byteBuffer).toString();
                System.out.println("Success Get message: " + message );

                byteBuffer = charset.encode("Hello Client!!");
                socketChannel.write(byteBuffer);
                System.out.println("Success send message!!");
            }

        }catch(Exception e) {}

        if(serverSocketChannel.isOpen())
        {try{serverSocketChannel.close();
        } catch (IOException e1) {}
        }
    }
}

