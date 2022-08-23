package com.example.niosocket;
/*selector 사용 챗팅서버 구현 2022-08-23
*  non-blocking ServerSocketChannel 사용방법 학습*/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SelectorServer extends Application {

    Selector selector;
    ServerSocketChannel serverSocketChannel;
    List<Client> connections = new Vector<Client>();

    // Selector 생성, non-blocking ServersocketChannel 생성 , port binding, register selector, work thread 생성
    void startServer() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false); //non-blocking
            serverSocketChannel.bind(new InetSocketAddress(5001));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //Selector에 작업 유형 등록
        } catch(Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();}

            return;
        }

        Thread thread = new Thread(){
            @Override
            public void run() {
                while(true) {
                    try {
                        int keyCount = selector.select(); // 작업 처리 준비가된 채널이 있을 때까지 대기
                        if(keyCount == 0) {continue;}
                        Set<SelectionKey> selectionKeys = selector.selectedKeys(); // 작업처리 준비가 된 키를 얻고 Set collection으로 리턴
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();

                        //반복자를 반복하면서 SelectionKey를 하나씩 꺼낸다.
                        while(iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            if (selectionKey.isAcceptable()) { //연락 수락 잡업일 경우
                                accept(selectionKey);
                            } else if (selectionKey.isReadable()){
                                Client client = (Client)selectionKey.attachment(); //Selection keys therefore support
                                // the attachment of a single arbitrary object to a key. An object can be attached via the attach method
                                // and then later retrieved via the attachment method.
                                client.receive(selectionKey);
                            } else if(selectionKey.isWritable()) {
                                Client client = (Client)selectionKey.attachment();
                                client.send(selectionKey);
                            }
                            iterator.remove(); // 선택된 키셋에서 처리 완료된 SelectionKey 제거
                        }
                    } catch (Exception e) { // 예외발생시 stopServer method 호출
                        if (serverSocketChannel.isOpen()) { stopServer();}
                        break;
                    }
                }
            }
        };
        thread.start();
        Platform.runLater(()-> {displayText("Server Start!!!");
        btnStartStop.setText("stop");
        });
    }

    // SocketChannel 닫기, ServerSocketChannel close, selector close 기능 with "stop" button
    void stopServer() {
        try {
            Iterator<Client> iterator = connections.iterator();
            while(iterator.hasNext()) {
                Client client = iterator.next();
                client.socketChannel.close(); //연결된 SocketChannel 닫기
                iterator.remove();
            }
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
            if(selector!=null && selector.isOpen()) {
                selector.close();}

            Platform.runLater(()->{
                displayText("Server Stop!!!");
                btnStartStop.setText("start");
            });
        } catch (Exception e) {}
    }

    //연결 수락 코드, SelectionKey의 isAcceptable()이 true를 리턴하면m accept() method 는 연결을 수락 Client 객체 생성
    void accept(SelectionKey selectionKey) {
        try {
            ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) selectionKey.channel(); //ServerSocketChannel 얻기
            SocketChannel socketChannel = serverSocketChannel1.accept(); // 연결 수락 후 SocketChannel을 리턴
            String message = "Accept request" + socketChannel.getRemoteAddress() + ":" + Thread.currentThread().getName() + "!!";
            Platform.runLater(()->displayText(message));

            Client client = new Client(socketChannel); // 객체 생성 및 저장
            connections.add(client);

            Platform.runLater(()-> displayText("Number of connections : " + connections.size() + "."));
            } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {stopServer();}
        }
        }



    //Data 통신 코드
    class Client {
        SocketChannel socketChannel;
        String sendData; // Client로 보낼 데이터를 저장하는 필드

        //생성자 선언
        Client(SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ); // 작업유형 설정

            selectionKey.attach(this); //SelectionKey에 자기 자신(client)을 첨부 객체로 저장
        }

        void receive(SelectionKey selectionKey) {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(100);

                //상대방이 비정상 종료했을 경우 자동으로 IOException 발생
                int byteCount = socketChannel.read(byteBuffer); //Get data
                if (byteCount == -1) {
                    throw new IOException();
                }

                String message = "Request process" + socketChannel.getRemoteAddress() + ":" + Thread.currentThread() + ".";
                Platform.runLater(() -> displayText(message));

                byteBuffer.flip(); //문자열 변환 부분
                Charset charset = Charset.forName("UTF-8");
                String data = charset.decode(byteBuffer).toString();

                for (Client client : connections) {
                    client.sendData = data;
                    SelectionKey key = client.socketChannel.keyFor(selector);
                    key.interestOps(SelectionKey.OP_WRITE); //작업유형 변경
                }
                selector.wakeup(); // select() method blocking이 해제, 변경된 작업유형을 감지하도록 select() method 다시 호출된다.
            } catch (Exception e) {
                try {
                    connections.remove(this);
                    String message = "Client connection disable" + socketChannel.getRemoteAddress() + ":" + Thread.currentThread().getName() + ".";
                    Platform.runLater(() -> displayText(message));
                    socketChannel.close();
                } catch (IOException e2) {
                }
            }
        }

        void send(SelectionKey selectionKey) {
            try {
                Charset charset = Charset.forName("UTF-8");
                ByteBuffer byteBuffer = charset.encode(sendData);
                socketChannel.write(byteBuffer);
                selectionKey.interestOps(SelectionKey.OP_READ);
                selector.wakeup(); //
            } catch (Exception e) {
                try {
                    connections.remove(this);
                    String message = "Client connection disable : " + socketChannel.getRemoteAddress() + " : " + Thread.currentThread().getName() + ".";
                    Platform.runLater(() -> displayText(message));
                    socketChannel.close();
                } catch (Exception e2) {
                }
            }
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
