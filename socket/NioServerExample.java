package com.example.niosocket;
/*java nio 를 사용한 챗팅 -2022-08-22*/


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class NioServerExample extends Application {

    ExecutorService executorService;
    ServerSocketChannel serverSocketChannel;
    List<Client> connections = new Vector<Client>(); //연결된 Client를 저장하는 필드

    /*실행 화면에서 start 버튼을 클릭하면 startServer 메소드가 호출*/
    void startServer() {/*server start Code, ExecutorService 생성, ServerSocketChannel 생성*/
        executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        //5001 port에 클라이언트 연결을 수락하는 ServerSocketChannel을 생성

        try{
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.bind(new InetSocketAddress(5001));
        } catch (Exception e) {
            if(serverSocketChannel.isOpen()) { stopServer();}
                return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    displayText("[Server Start]");
                    btnStartStop.setText("stop");
                });
                while (true) {
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept(); //연결 수락
                        String message = "[ Accept Connection: " + socketChannel.getRemoteAddress() +
                                ": " + Thread.currentThread().getName() +"]";
                        Platform.runLater(() -> displayText(message));
                        
                        Client client = new Client(socketChannel); // Client 객체를 생성하고 connections 컬렉션에 추가
                        connections.add(client);
                        
                        Platform.runLater(() -> displayText("Number of Connections : " +  connections.size() + "]"));
                    } catch (Exception e) {
                        if (serverSocketChannel.isOpen()) {stopServer();}
                        break;
                    }
                }
            }
        };
        executorService.submit(runnable); //연락 잡업을 hread pool 에서 처리 하기 위한 호출
    }
    void stopServer() {

        try {
            Iterator<Client> iterator = connections.iterator();
            while(iterator.hasNext()) {         //while 문으로 반복자를 반복하면서 Client 를 하나씩 얻는다.
                Client client = iterator.next();
                client.socketChannel.close();
                iterator.remove();
            }
            if(serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
            if(executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            //ui 처리 부분
            Platform.runLater(()-> {    // 작업 스레드는 UI를 변경하지 못하므로 stop button을 Start로 변경
                displayText("[Server Stop]");
                btnStartStop.setText("start");
            });
        }catch ( Exception e){}
    }

    //서버는 다수의 클라이언트가 연결하기 때문에 클라이언트를 관리해야 한다. 클라이언트 별로 고유한 데이터를 저장하는 경우도 있다.
    class Client{ /*Data 통신 코드*/
        SocketChannel socketChannel; //SocketChannel을 필드로 선언

        Client(SocketChannel socketChannel){
            this.socketChannel = socketChannel; //Socketchannel 필드 초기화화
            receive();
        }
        void receive() { // Data 받는 코드, client가 보내는 데이터를 반복적으로 받기위해서 무한 루프를 돌면서 read 메소드 호출

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(100);

                            int readByteCount = socketChannel.read(byteBuffer);

                            // Client가 정상적으로 Socketchannel close를 호출한 경우
                            if(readByteCount == -1) {
                                throw new IOException(); // 클라이언트가 정상적으로 SocketChannel의 close를 호출했을 경우 read method return -1,
                                // 이 경우 IOException 강제로 발현
                            }

                            String message = "[Process of requirement: " + socketChannel.getRemoteAddress() + ":" +
                                    Thread.currentThread().getName() + "]";
                            Platform.runLater(()-> displayText(message));

                            byteBuffer.flip();
                            Charset charset = Charset.forName("UTF-8"); //UTF-8로 디코딩된 문자열을 얻는 과정
                            String data = charset.decode(byteBuffer).toString();

                            for(Client client : connections) {
                                client.send(data);
                            }
                        } catch (Exception e) {
                            try {
                                connections.remove(Client.this);
                                String message = "[Client Communication Error: " + socketChannel.getRemoteAddress() +":"+
                                        Thread.currentThread().getName() + "]";
                                Platform.runLater(() -> displayText(message));
                                socketChannel.close();
                            } catch (Exception e2) {
                            }break; //통신이 안될경우 루프를 빠져나감
                        }
                    }
                }
            };
            executorService.submit(runnable); //thread 작업처리
             }
        void send(String data) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try{
                        Charset charset = Charset.forName("UTF-8");
                        ByteBuffer byteBuffer = charset.encode(data);
                        socketChannel.write(byteBuffer);
                    } catch (Exception e){
                        try {
                            String message = "[Client Coummnication error: " + socketChannel.getRemoteAddress() + ":" +
                                    Thread.currentThread().getName() + "]";
                            Platform.runLater(() -> displayText(message));
                            connections.remove(Client.this); //connections collection에서 예외가 발생한 client 제거
                            socketChannel.close();
                        } catch (Exception e2) {}
                    }
                }
            };executorService.submit(runnable);
        }
    }




    /** UI 생성코드 **/
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
