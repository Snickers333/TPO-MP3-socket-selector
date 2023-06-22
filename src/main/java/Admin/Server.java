package Admin;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {

    private List<Topic> topics;
    private Map<SocketChannel, List<Topic>> subscriptions;
    private Selector selector;
    private ByteBuffer buffer;
    private ServerSocketChannel serverSocketChannel;

    public Server() {
        loadTopics();
        subscriptions = new HashMap<>();
        buffer = ByteBuffer.allocate(1024);

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            System.err.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
            System.exit(1);
        }
        serviceConnections();
    }

    private void serviceConnections() {
        boolean serverIsRunning = true;

        while (serverIsRunning) {
            try {
                selector.select();
                Set keys = selector.selectedKeys();
                Iterator iter = keys.iterator();
                while (iter.hasNext()) {

                    SelectionKey key = (SelectionKey) iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        SocketChannel cc = serverSocketChannel.accept();
                        cc.configureBlocking(false);
                        cc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        continue;
                    }

                    if (key.isReadable()) {
                        SocketChannel cc = (SocketChannel) key.channel();
                        serviceRequest(cc);
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private void serviceRequest(SocketChannel sc) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Charset charset = StandardCharsets.UTF_8;
        StringBuilder sb = new StringBuilder();

        try {
            while (sc.read(buffer) > 0) {
                buffer.flip();
                sb.append(charset.decode(buffer));
                buffer.clear();
            }
        } catch (SocketException e) {
            sc.close();
        }

        String[] message = sb.toString().trim().split(":");
        if (message[0].equals("getlist")) {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(topics);
            byte[] data = byteOut.toByteArray();
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
            sc.write(buffer);
            return;
        }
        if (message[0].equals("add")) {
            Topic topic = new Topic(message[1]);
            this.topics.add(topic);
            saveTopics();
            return;
        }
        if (message[0].equals("remove")) {
            Topic topic = new Topic(message[1]);
            subscriptions.entrySet()
                    .removeIf(e -> e.getValue()
                            .removeIf(t -> t.equals(topic)));
            topics.removeIf(t -> t.equals(topic));
            saveTopics();
            return;
        }
        if (message[0].equals("news")) {
            Topic topic = new Topic(message[1]);
            String information = message[2];
            for (Map.Entry<SocketChannel, List<Topic>> entry : subscriptions.entrySet()) {
                SocketChannel clientSocket = entry.getKey();
                List<Topic> clientTopics = entry.getValue();
                if (clientTopics.contains(topic)) {
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(byteOut);
                    String tmp = "news:" + topic + ":" + information;
                    Topic string = new Topic(tmp);
                    out.writeObject(string);
                    byte[] data = byteOut.toByteArray();
                    buffer.putInt(data.length);
                    buffer.put(data);
                    buffer.flip();
                    clientSocket.write(buffer);
                }
            }
            return;
        }
        if (message[0].equals("sub")) {
            if (subscriptions.containsKey(sc)){
                subscriptions.get(sc).add(new Topic(message[1]));
            } else {
                subscriptions.put(sc, new ArrayList<>());
                subscriptions.get(sc).add(new Topic(message[1]));
            }
            System.out.println("subscribed topic");
            return;
        }
        if (message[0].equals("unsub")) {
            if (subscriptions.containsKey(sc)) {
                subscriptions.get(sc).remove(new Topic(message[1]));
            }
            return;
        }
    }

    private void loadTopics() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("topics.txt"));
            String line = reader.readLine();
            topics = new ArrayList<>();
            while (line != null) {
                Topic topic = new Topic(line);
                topics.add(topic);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Błąd podczas ładowania listy Topic: " + e.getMessage());
            System.exit(1);
        }
    }

    private void saveTopics() {
        try (FileWriter fw = new FileWriter("topics.txt")) {
            for (Topic t : topics) {
                fw.write(t + "\n");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
    }
}
