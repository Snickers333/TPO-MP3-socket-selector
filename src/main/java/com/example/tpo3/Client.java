package com.example.tpo3;

import Admin.Topic;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Client extends Application {
    private ObservableList<Topic> topicNames;
    private ListView<Topic> topicListView;
    private ObservableList<Topic> subsNames;
    private ListView<Topic> subsListView;
    private TextArea infoTextArea;
    private SocketChannel sc;
    private Thread readThread;
    private boolean listening = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        topicNames = FXCollections.observableArrayList();
        try {
            sc = SocketChannel.open(new InetSocketAddress("localhost", 8080));
            startListener();
            command("getlist:");
        } catch (IOException e) {
            topicNames.add(new Topic("Error: No connection with server."));
        }
        topicListView = new ListView<>(topicNames);

        subsNames = FXCollections.observableArrayList();
        subsListView = new ListView<>(subsNames);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10, 10, 50, 10));

        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(event -> {
            topicNames.clear();
            command("getlist:");
        });

        Button subsButton = new Button("Subscribe"); //TODO duplicates subscribe
        subsButton.setOnAction(e -> {
            Topic topic = topicListView.getSelectionModel().getSelectedItem();
            if (topic != null) {
                command("sub:" + topic);
                subsListView.getItems().add(topic);
            }
        });

        Button unSubsButton = new Button("Unsubscribe");
        unSubsButton.setOnAction(e -> {
            Topic topic = subsListView.getSelectionModel().getSelectedItem();
            if (topic != null) {
                command("unsub:" + topic);
                subsListView.getItems().remove(topic);
            }
        });

        VBox localBox = new VBox();
        localBox.setAlignment(Pos.CENTER);

        localBox.getChildren().addAll(subsListView, unSubsButton);

        VBox InfoBox = new VBox();
        InfoBox.setSpacing(10);
        InfoBox.setPadding(new Insets(10));
        InfoBox.setAlignment(Pos.CENTER);

        Label infoLabel = new Label("Informacja:");
        infoTextArea = new TextArea();
        infoTextArea.setWrapText(true);

        InfoBox.getChildren().addAll(infoLabel, infoTextArea);

        HBox buttonsBox = new HBox(refreshButton, subsButton);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setSpacing(10);

        VBox ServerBox = new VBox(topicListView, buttonsBox);
        ServerBox.setAlignment(Pos.CENTER);
        root.setBottom(InfoBox);

        SplitPane centerPane = new SplitPane();
        centerPane.getItems().addAll(ServerBox, localBox);

        root.setCenter(centerPane);
        root.setTop(localBox);
        primaryStage.setScene(new Scene(root, 600, 407));
        primaryStage.show();
    }
    private void command(String message) {
        if (sc != null || sc.isConnected()) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                sc.write(buffer);
            } catch (IOException e) {
                System.out.println("Command exception");
            }
        }
    }
    private void getList(byte[] bytes) {
        try {
            List<Topic> list = (List<Topic>) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
            topicNames.addAll(list);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private void startListener() {
        readThread = new Thread(() -> {
            try {
                listening = true;
                while (listening) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    sc.read(buffer);
                    buffer.flip();
                    byte[] data = new byte[buffer.getInt()];
                    buffer.get(data);
                    Platform.runLater(() -> {process(data);});
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readThread.start();
    }

    private void process(byte[] bytes) {
        String response = new String(bytes, StandardCharsets.UTF_8);
        String[] message = response.split(":");
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        Topic topic = new Topic("lol");
        try {
            ois = new ObjectInputStream(bis);
            topic = (Topic) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.out.println("lista");
        }
        String[] arr = topic.toString().split(":");
        if (arr[0].equals("news")) {
            infoTextArea.appendText(message[2]);
        } else if (message[0].equals("refresh")){
            getList(bytes);
        } else {
            getList(bytes);
        }
    }

}