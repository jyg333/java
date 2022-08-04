package socketOfServer.dataCommunication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerExample {
    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("localhost", 5001)); //port 와 binding

            while (true){
                //계속해서 클라이언트의 요청을 기다리기 위한 무한루프
                System.out.println("Wait connection with client");
                Socket socket = serverSocket.accept(); //blocking before connection
                InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress(); //(InetSocketAddress) : type 변환
                System.out.println("Accept Connection" + isa.getHostName()); //Get client IP address

                byte[] bytes = null;
                String message = null;

                InputStream is = socket.getInputStream();
                bytes = new byte[100];
                int readByteCount = is.read(bytes); //client 가 데이터를 보내기전까지 blocking status
                message = new String(bytes,0, readByteCount, "UTF-8");
                System.out.println("Success get data : " + message);

                OutputStream os = socket.getOutputStream();
                message = "hello Client";
                bytes = message.getBytes("UTF-8"); // message 로 부터 bytes배열을 얻어낸다
                os.write(bytes);
                os.flush();
                System.out.println("Success sending Data!!");

                is.close();
                os.close();
                socket.close(); //연결을 끝겠다는 의미



            }

        } catch (Exception e) {
        }

        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
