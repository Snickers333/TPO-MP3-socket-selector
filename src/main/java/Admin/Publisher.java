package Admin;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Publisher extends Application {
    private ObservableList<Topic> topicNames;
    private TextArea infoTextArea;
    private SocketChannel sc;
    private ListView<Topic> topicListView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        topicNames = FXCollections.observableArrayList();
        topicNames.addAll(Reconnect());

        topicListView = new ListView<>(topicNames);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10, 10, 50, 10));
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        Label titleLabel = new Label("Tytuł:");
        TextField titleField = new TextField();
        form.addRow(0, titleLabel, titleField);

        Button addButton = new Button("Dodaj");
        addButton.setOnAction(event -> {
            String title = titleField.getText().trim();
            if (!title.isEmpty() && sc != null) {
                Topic newTopic = new Topic(title);
                try {
                    command("add:" + newTopic, false);
                    topicListView.getItems().add(newTopic);
                    titleField.clear();
                } catch (IOException e) {
                    topicNames.clear();
                    topicNames.add(new Topic("Error: No Connection with server."));
                }
            }
        });

        Button removeButton = new Button("Usuń");
        removeButton.setOnAction(event -> {
            Topic selectedTopic = topicListView.getSelectionModel().getSelectedItem();
            if (selectedTopic != null) {
                try {
                    command("remove:" + selectedTopic, false);
                    topicListView.getItems().remove(selectedTopic);
                } catch (IOException e) {
                    topicNames.clear();
                    topicNames.add(new Topic("Error: No Connection with server."));
                }
            }
        });

        Button refreshButton = new Button("Odśwież");
        refreshButton.setOnAction(event -> {
            topicNames.clear();
            topicNames.addAll(Reconnect());
        });

        VBox announceBox = new VBox();
        announceBox.setSpacing(10);
        announceBox.setPadding(new Insets(10));
        announceBox.setAlignment(Pos.CENTER);

        Label infoLabel = new Label("Informacja:");
        infoTextArea = new TextArea();
        infoTextArea.setWrapText(true);

        Button announceButton = new Button("Ogłoszenie");
        announceButton.setOnAction(event -> {
            Topic selectedTopic = topicListView.getSelectionModel().getSelectedItem();
            String information = infoTextArea.getText().trim();
            if (selectedTopic != null && !information.isEmpty()) {
                try {
                    command("news:" + selectedTopic + ":" + information, false);
                    infoTextArea.clear();
                } catch (IOException e) {
                    topicNames.clear();
                    topicNames.add(new Topic("Error: No Connection with server."));
                }
            }
        });

        announceBox.getChildren().addAll(infoLabel, infoTextArea, announceButton);
        VBox refreshBox = new VBox(topicListView, refreshButton);
        refreshBox.setAlignment(Pos.CENTER);
        root.setBottom(announceBox);
        SplitPane centerPane = new SplitPane();
        centerPane.getItems().addAll(refreshBox, form);
        centerPane.setDividerPositions(0.3);

        root.setCenter(centerPane);
        form.addRow(2, addButton, removeButton);
        root.setTop(form);
        primaryStage.setScene(new Scene(root, 600, 407));
        primaryStage.show();
    }

    private List<Topic> Reconnect() {
        List<Topic> topics;
        try {
            topics = (List<Topic>) command("getlist:", true).readObject();
        } catch (NullPointerException | IOException | ClassNotFoundException e) {
            topics = new ArrayList<>();
            topics.add(new Topic("Error: Did not receive any topics."));
        }
        return topics;
    }

    private ObjectInputStream command(String message, boolean read) throws IOException {
        if (sc == null || !sc.isConnected()) {
            sc = SocketChannel.open(new InetSocketAddress("localhost", 8080));
        }
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        sc.write(buffer);
        if (read) {
            buffer = ByteBuffer.allocate(1024);
            sc.read(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.getInt()];
            buffer.get(data);
            return new ObjectInputStream(new ByteArrayInputStream(data));
        }
        return null;
    }
}