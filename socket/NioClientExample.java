package com.example.niosocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class NioClientExample extends Application{
    SocketChannel socketChannel;

    void startClient() {
        Thread thread = new Thread(){
            @Override
            public void run() {
                try{
                    //Socket 생성 및 연결 요청 부분
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress("localhost", 5001));
                    Platform.runLater(() ->{
                        try {
                            displayText("[Connection Success: " + socketChannel.getRemoteAddress() + "]");
                            btnConn.setText("Stop");
                            btnSend.setDisable(false); // send 버튼 활성화
                        } catch(Exception e){}

                    });
                } catch (Exception e) {
                    Platform.runLater(() -> displayText("[Server Communication disable!!"));
                    if(socketChannel.isOpen()) {stopClient();}
                    return; // thread 종료
                }
                receive(); // 예외가 발생하지 않으면 receive 메소드를 호출하여 서버에서 보낸 데이터 받기
            }
        };
        thread.start();
    }
    void stopClient() {
        try {
            Platform.runLater(() ->{
                displayText("[Close connection!!]");
                btnConn.setText("Start");
                btnSend.setDisable(true); //버튼 비활성화
            });
            if(socketChannel !=null && socketChannel.isOpen()){ // 필드가 null이 아니고 현재 열려 있는 경우
                socketChannel.close();
            }
        } catch (Exception e) {}
    }
    void receive() { //startClient에서 작성한 작업 스레드 상에서 호출됨
        while(true) {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(100);

                //server가 비정상적으로 종료했을 경우 IOException 발생
                int readByteCount = socketChannel.read(byteBuffer); // 데이터 받기

                //server가 정상적으로 Socket 에서 close호출
                if(readByteCount == -1){throw new IOException();
                }
                byteBuffer.flip();
                Charset charset = Charset.forName("UTF-8");
                String data = charset.decode(byteBuffer).toString(); // 문자열 반환
                Platform.runLater(() -> displayText("[Complete receive!" + data));
            } catch (Exception e) {
                Platform.runLater(() -> displayText("Server Communication disable!!"));
                stopClient();
                break;
            }
        }
    }
    void send(String data) {
        Thread thread = new Thread(){
            @Override
            public void run(){
                try{
                    Charset charset = Charset.forName("UTF-8");
                    ByteBuffer byteBuffer = charset.encode(data);
                    socketChannel.write(byteBuffer);
                    Platform.runLater(()->displayText("Sending Success"));
                }catch (Exception e) {
                    Platform.runLater(()->displayText("Server Communication Disable!"));
                    stopClient();
                }
            }
        };
        thread.start();
    }



    /* UI 생성 코드 */
    TextArea txtDisplay;
    TextField txtInput;
    Button btnConn, btnSend;

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        root.setPrefSize(500, 300);

        txtDisplay = new TextArea();
        txtDisplay.setEditable(false);
        BorderPane.setMargin(txtDisplay, new Insets(0, 0, 2, 0));
        root.setCenter(txtDisplay);

        BorderPane bottom = new BorderPane();
        txtInput = new TextField();
        txtInput.setPrefSize(60, 30);
        BorderPane.setMargin(txtInput, new Insets(0, 1, 1, 1));

        btnConn = new Button("start");
        btnConn.setPrefSize(60, 30);
        btnConn.setOnAction(e->{
            if (btnConn.getText().equals("start")) {
                startClient();
            } else if (btnConn.getText().equals("stop")) {
                stopClient();
            }
        });

        btnSend = new Button("send");
        btnSend.setPrefSize(60, 30);
        btnSend.setDisable(true);
        btnSend.setOnAction(e->send(txtInput.getText()));

        bottom.setCenter(txtInput);
        bottom.setLeft(btnConn);
        bottom.setRight(btnSend);
        root.setBottom(bottom);

        Scene scene = new Scene(root);
//        scene.getStylesheets().add(getClass().getResource("app.css").toString());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client");
        primaryStage.setOnCloseRequest(event->stopClient());
        primaryStage.show();
    }

    void displayText(String text) {
        txtDisplay.appendText(text + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
