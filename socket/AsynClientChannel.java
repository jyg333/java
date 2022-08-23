package com.example.niosocket;
/*  비동기 클라이언트 채널 만들기 2022-08-23*/

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AsynClientChannel extends Application {
    AsynchronousChannelGroup channelGroup;
    AsynchronousSocketChannel socketChannel;

    void startClient() {
        try {
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    Executors.defaultThreadFactory()
            );

            socketChannel = AsynchronousSocketChannel.open(channelGroup);
            socketChannel.connect(new InetSocketAddress("localhost", 5001), null, new CompletionHandler<Void,Void>(){
                @Override
                public void completed(Void result, Void attachment) {
                    Platform.runLater(()->{
                        try {
                            displayText("[연결 완료 : " + socketChannel.getRemoteAddress() + " : " + Thread.currentThread().getName() + "]");
                            btnConn.setText("stop");
                            btnSend.setDisable(false);
                        } catch (Exception e) {}
                    });
                    receive(); // 서버에서 보낸 데이터 받기
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    Platform.runLater(()->displayText("[서버 통신 안 됨]"));
                    if (socketChannel.isOpen()) stopClient();
                }
            });
        } catch (Exception e) {}
    }

    void stopClient() {
        try {
            Platform.runLater(()->{
                displayText("[연결 끊음]");
                btnConn.setText("start");
                btnSend.setDisable(true);
            });

            if (channelGroup != null && !channelGroup.isShutdown()) {
                channelGroup.shutdownNow();
            }
        } catch (Exception e) {}
    }

    void receive() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        socketChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer,ByteBuffer>(){
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                attachment.flip();
                Charset charset = Charset.forName("UTF-8");

                String data = charset.decode(attachment).toString();
                Platform.runLater(()->displayText("[받기 완료] " + data));

                ByteBuffer byteBuffer = ByteBuffer.allocate(100);
                socketChannel.read(byteBuffer, byteBuffer, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                Platform.runLater(()->displayText("[서버 통신 안 됨]"));
                stopClient();
            }
        });
    }

    void send(String data) {
        Charset charset = Charset.forName("UTF-8");
        ByteBuffer byteBuffer = charset.encode(data);
        socketChannel.write(byteBuffer, null, new CompletionHandler<Integer,Void>(){
            @Override
            public void completed(Integer result, Void attachment) {
                Platform.runLater(()->displayText("[보내기 완료]"));
                txtInput.clear();
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                Platform.runLater(()->displayText("[서버 통신 안 됨]"));
                stopClient();
            }
        });
    }

    /****************
     * UI 생성 코드 */
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
