package com.example.niosocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
/* -챗팅버서 구현
*  비동기 서버 채널 만들기 2022-08-23
*  같은 Thread pool을 공유하는 비동기 채널들의 묶음
*  1. 비동기 채널을 생성할 때 채널 그룹을 지정하지 않으면 기본 비동기 채널그룹이 생성
*    비동기 채널 그룹은 비동기 채널을 새엉할 때 매개값으로 사용됨*/

public class AsynServerChannel extends Application {

    AsynchronousChannelGroup channelGroup; //field 선언
    AsynchronousServerSocketChannel serverSocketChannel; //client연결 수락
    List<Client> connections = new Vector<Client>();


    //비동기 그룹 생성, 비동기 서버소켓 채널 생성 및 포트 바인딩
    void startServer() {
        try {
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    Executors.defaultThreadFactory()
            );
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(5001));
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
            return;
        }
        Platform.runLater(() -> {
            displayText("Server Start!");
            btnStartStop.setText("stop");

        });

        // 첨부객체 null, <AsynchronousSocketChannel : 결과 타입, Void : 첨부타임>
        serverSocketChannel.accept(null,
                new CompletionHandler<AsynchronousSocketChannel, Void>() {
            //연락 수락 잡업이 성공되었을 때 콜백되는 completed를 재정의
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                try {
                    String message = "Accept Connection: " + socketChannel.getRemoteAddress() +":" + Thread.currentThread().getName()+ ".!!";
                    Platform.runLater(() -> displayText(message));
                } catch (IOException e) {}
                
                //Client  생성  -> 객체 저장
                Client client = new Client(socketChannel);
                connections.add(client);
                Platform.runLater(() -> displayText("Number of connected : "+ connections.size() +"."));
                
                serverSocketChannel.accept(null, this);
            }
            //연락 수락 잡업이 실패되었을때 콜백되는 failed 재정의
            @Override
            public void failed(Throwable exc, Void attachment) {
                if(serverSocketChannel.isOpen()) {stopServer();}
                
            }       
        });
        
    }
    void stopServer() {
        try {
            connections.clear(); //connections 컬랙션에 있는 모든 Client를 제거
            if(channelGroup !=null && !channelGroup.isShutdown()) {
                channelGroup.shutdown(); //비동기 채널 그룹 종료
            }
            Platform.runLater(() ->{
                displayText("Server Stopped");
                btnStartStop.setText("start");
            });
        } catch (Exception e) {}
    }

    // 클라이언트별로 고유한 데이터를 저장할 필요성도 있다. 연락 수락 시 마다 Client 인스턴스를 생성해서 관리하는 것이 좋다.
    //Client class에는 Data 받기& 보내기 기능이 필요하다.
    class Client{
        AsynchronousSocketChannel socketChannel;

        //AsynchronousSocketChannel 필드 초기화 후 receive() method 호출
        Client(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            receive();
        }
        //Receiving Data
        void receive() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(100);
            // 첫번째 매개값은 데이터를 저장할 bytebuffer, 두번째는 콜백 매소드의 매개값으로 전달할 첨부객체 bytebuffer
            socketChannel.read(byteBuffer, byteBuffer,
                    //결과 타입 :integer, 첨부타입 : ByteBuffer
                    new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        //읽기잡업 성공되었을때 콜백되는 completed() 재정의
                        public void completed(Integer result, ByteBuffer attachment) {
                            try {
                                String message = "Request Process" + socketChannel.getRemoteAddress() + ":" + Thread.currentThread().getName() + "!!";
                                Platform.runLater(() ->
                                        displayText(message));

                                //문자열 변환
                                attachment.flip();
                                Charset charset = Charset.forName("UTF-8");
                                String data = charset.decode(attachment).toString();

                                // 저장된 Client를 하나씩 얻어 모든 클라이언트에게 보내기
                                for(Client client : connections) {
                                    client.send(data);
                                }
                                ByteBuffer byteBuffer1 = ByteBuffer.allocate(100);
                                socketChannel.read(byteBuffer,byteBuffer, this); // 다시 데이터 읽기
                            } catch (Exception e) {}
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            try {
                                String message = "Client Communication disable : " + ":" + Thread.currentThread().getName() +"!";
                                Platform.runLater(() ->displayText(message));
                                connections.remove(Client.this);
                                socketChannel.close();
                            } catch(IOException e) {}
                        }
                    });
                }

        void send(String data){
            Charset charset = Charset.forName("UTF-8");
            ByteBuffer byteBuffer = charset.encode(data);
            socketChannel.write(byteBuffer, null,
                    new CompletionHandler<Integer, Object>() {
                        @Override
                        public void completed(Integer result, Object attachment) {

                        }

                        @Override
                        public void failed(Throwable exc, Object attachment) {
                            try {
                                String message = "Client Communication disable : " + ":" + Thread.currentThread().getName() + "!";
                                Platform.runLater(() -> displayText(message));
                                connections.remove(Client.this);
                                socketChannel.close();

                            } catch (IOException e) {
                            }

                        }
                    });
        }
    }






    // UI 생성코드
    TextArea txtDisplay;
    Button btnStartStop;

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        root.setPrefSize(500, 300);

        txtDisplay = new TextArea();
        txtDisplay.setEditable(false);
        BorderPane.setMargin(txtDisplay, new Insets(0, 0, 2, 0));
        root.setCenter(txtDisplay);

        btnStartStop = new Button("start");
        btnStartStop.setPrefHeight(30);
        btnStartStop.setMaxWidth(Double.MAX_VALUE);
        btnStartStop.setOnAction(e->{
            if (btnStartStop.getText().equals("start")) {
                startServer();
            } else if (btnStartStop.getText().equals("stop")) {
                stopServer();
            }
        });
        root.setBottom(btnStartStop);

        Scene scene = new Scene(root);
//        scene.getStylesheets().add(getClass().getResource("app.css").toString());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server");
        primaryStage.setOnCloseRequest(event->stopServer());
        primaryStage.show();
    }

    void displayText(String text) {
        txtDisplay.appendText(text + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
