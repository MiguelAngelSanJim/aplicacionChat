package org.example.demo4;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.*;
import java.io.*;
import java.util.function.Consumer;

public class ChatModel {
    private static final int PORT = 9876;
    private static final String BROADCAST_IP = "192.168.182.255";

    private DatagramSocket socket;
    private String userName;
    private ObservableList<String> connectedUsers;

    public ChatModel(String userName) throws IOException {
        this.userName = userName;
        connectedUsers = FXCollections.observableArrayList();
        Platform.runLater(() -> connectedUsers.add(userName));

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        socket.setBroadcast(true);

        // Al cerrar la aplicación (por ejemplo, al pulsar la X), se envía el mensaje de desconexión y se cierra el socket.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                sendMessage(userName + " se ha desconectado.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            close();
        }));
    }

    public void sendMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_IP);
        DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
        socket.send(packet);
    }

    public void startReceiver(Consumer<String> onMessageReceived) {
        Thread receiverThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        socket.receive(packet);
                    } catch (SocketException se) {
                        if (socket.isClosed()) {
                            return;
                        } else {
                            throw se;
                        }
                    }
                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("Usuarios conectados: [")) {
                        updateUserList(message);
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    } else if (message.endsWith("se ha conectado.")) {
                        String newUser = message.split(" ")[0];
                        Platform.runLater(() -> {
                            if (!connectedUsers.contains(newUser)) {
                                connectedUsers.add(newUser);
                            }
                        });
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    } else if (message.endsWith("se ha desconectado.")) {
                        String disconnectedUser = message.split(" ")[0];
                        Platform.runLater(() -> connectedUsers.remove(disconnectedUser));
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    } else {
                        if (message.startsWith(userName + ":")) {
                            Platform.runLater(() -> onMessageReceived.accept("mensaje (eco): " + message));
                        } else {
                            Platform.runLater(() -> onMessageReceived.accept(message));
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    e.printStackTrace();
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void updateUserList(String message) {
        int startIndex = message.indexOf('[');
        int endIndex = message.indexOf(']');
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return;
        }
        String usersSubstring = message.substring(startIndex + 1, endIndex);
        String[] userArray = usersSubstring.split(",");
        Platform.runLater(() -> {
            connectedUsers.clear();
            for (String user : userArray) {
                String trimmedUser = user.trim();
                if (!trimmedUser.isEmpty()) {
                    connectedUsers.add(trimmedUser);
                }
            }
        });
    }

    public ObservableList<String> getConnectedUsers() {
        return connectedUsers;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
