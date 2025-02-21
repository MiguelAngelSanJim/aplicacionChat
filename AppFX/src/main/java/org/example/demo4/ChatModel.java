package org.example.demo4;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.net.*;
import java.io.*;
import java.util.function.Consumer;

public class ChatModel {
    private static final int PORT = 9876;
    private static final String BROADCAST_IP = "192.168.182.255";

    private DatagramSocket socket;
    private String userName;
    private ObservableList<User> connectedUsers;

    public ChatModel(String userName) throws IOException {
        this.userName = userName;
        connectedUsers = FXCollections.observableArrayList();
        // Agregar el usuario local (con su estado "En línea") a la lista en el hilo de JavaFX.
        Platform.runLater(() -> connectedUsers.add(new User(userName)));

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        socket.setBroadcast(true);

        // Shutdown hook: al cerrar la app, notificar desconexión y cerrar el socket.
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
                        if (socket.isClosed()) return;
                        else throw se;
                    }
                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("Usuarios conectados: [")) {
                        updateUserList(message);
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                    else if (message.endsWith("se ha conectado.")) {
                        String newUser = message.split(" ")[0];
                        if (!newUser.equals(userName)) {
                            Platform.runLater(() -> {
                                if (!containsUser(newUser))
                                    connectedUsers.add(new User(newUser));
                            });
                            // Enviar confirmación silenciosa (no se muestra en el chat)
                            try {
                                sendMessage("confirmacion: " + userName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                    else if (message.startsWith("confirmacion:")) {
                        String confirmUser = message.substring("confirmacion:".length()).trim();
                        if (!confirmUser.equals(userName)) {
                            Platform.runLater(() -> {
                                if (!containsUser(confirmUser))
                                    connectedUsers.add(new User(confirmUser));
                            });
                        }
                    }
                    else if (message.endsWith("se ha desconectado.")) {
                        String disconnectedUser = message.split(" ")[0].trim();
                        Platform.runLater(() -> connectedUsers.removeIf(u -> u.getName().equals(disconnectedUser)));
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                    else {
                        if (message.startsWith(userName + ":"))
                            Platform.runLater(() -> onMessageReceived.accept("mensaje (eco): " + message));
                        else
                            Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed())
                    e.printStackTrace();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private boolean containsUser(String name) {
        return connectedUsers.stream().anyMatch(u -> u.getName().equals(name));
    }

    private void updateUserList(String message) {
        int startIndex = message.indexOf('[');
        int endIndex = message.indexOf(']');
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return;
        String usersSubstring = message.substring(startIndex + 1, endIndex);
        String[] userArray = usersSubstring.split(",");
        Platform.runLater(() -> {
            connectedUsers.clear();
            for (String user : userArray) {
                String trimmedUser = user.trim();
                if (!trimmedUser.isEmpty())
                    connectedUsers.add(new User(trimmedUser));
            }
        });
    }

    public ObservableList<User> getConnectedUsers() {
        return connectedUsers;
    }

    public void close() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }

    // Clase para representar un usuario conectado.
    public static class User {
        private final String name;
        public User(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    // Celda personalizada para el ListView que muestra la lista de usuarios.
    // Para aplicarla, en el controlador, haz:
    // listView.setCellFactory(lv -> new ChatModel.UserCell());
    public static class UserCell extends ListCell<User> {
        private final HBox hbox = new HBox(5);
        private final Label nameLabel = new Label();
        private final Circle statusCircle = new Circle(5); // Radio de 5px.
        private final Label statusLabel = new Label("En línea");

        public UserCell() {
            // Asignar las clases de estilo para personalización desde CSS.
            nameLabel.getStyleClass().add("user-name");
            statusCircle.getStyleClass().add("status-circle");
            statusLabel.getStyleClass().add("status-label");
            hbox.getChildren().addAll(nameLabel, statusCircle, statusLabel);
        }

        @Override
        protected void updateItem(User item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                setGraphic(hbox);
            }
        }
    }
}
