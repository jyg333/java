package socketOfServer.dataCommunication;
//ServerExample class를 테스트하기 위해선 필수

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocket {
    public static void main(String[] args) {
        Socket socket = null;

        try {
            socket = new Socket();
            System.out.println("Asking connection");
            socket.connect(new InetSocketAddress("localhost", 5001));
            System.out.println("Connection Success");

            byte[] bytes = null;
            String message = null;

            OutputStream os = socket.getOutputStream();
            message = "Hello server";
            bytes = message.getBytes("UTF-8");
            os.write(bytes);
            os.flush();
            System.out.println("Sending data Success");

            InputStream is = socket.getInputStream();
            bytes = new byte[100];
            int readByteCount = is.read(bytes);
            message = new String(bytes, 0, readByteCount,"UTF-8"); //from 0 index to read bytes
            System.out.println("Success getting Data!!" + message);

            //더이상 서버와 통신할 일이 없는 경우 닫는다
            os.close();
            is.close();
//            socket.close();


        } catch (Exception e){
            e.printStackTrace();
        }
        if(!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}
