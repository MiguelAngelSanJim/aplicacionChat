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

/**
 * Modelo de chat que maneja la comunicación entre usuarios utilizando UDP broadcast.
 * Gestiona el envío y recepción de mensajes, así como la lista de usuarios conectados.
 */
public class ChatModel {
    private static final int PORT = 9876;
    private static final String BROADCAST_IP = "192.168.182.255";

    private DatagramSocket socket;
    private String userName;
    private ObservableList<User> connectedUsers;

    /**
     * Constructor del modelo de chat.
     *
     * @param userName Nombre del usuario que inicia el chat.
     * @throws IOException sí ocurre un error al inicializar el socket de comunicación.
     */
    public ChatModel(String userName) throws IOException {
        this.userName = userName;
        connectedUsers = FXCollections.observableArrayList();
        Platform.runLater(() -> connectedUsers.add(new User(userName)));

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        socket.setBroadcast(true);

        // Notificar desconexión al cerrar la app.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                sendMessage(userName + " se ha desconectado.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            close();
        }));
    }

    /**
     * Envía un mensaje a todos los usuarios conectados.
     *
     * @param message Mensaje a enviar.
     * @throws IOException sí ocurre un error durante el envío del mensaje.
     */
    public void sendMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_IP);
        DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
        socket.send(packet);
    }

    /**
     * Inicia el receptor de mensajes en un hilo separado.
     *
     * @param onMessageReceived Función de callback para procesar los mensajes recibidos.
     */
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
                    handleReceivedMessage(message, onMessageReceived);
                }
            } catch (IOException e) {
                if (!socket.isClosed())
                    e.printStackTrace();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * Procesa un mensaje recibido y actualiza la lista de usuarios o la interfaz, según corresponda.
     */
    private void handleReceivedMessage(String message, Consumer<String> onMessageReceived) {
        if (message.startsWith("Usuarios conectados: [")) {
            updateUserList(message);
            return; // Evitamos mostrar el mensaje de actualización de usuarios.
        }
        else if (message.endsWith("se ha conectado.")) {
            String newUser = message.split(" ")[0];
            if (!newUser.equals(userName)) {
                Platform.runLater(() -> {
                    if (!containsUser(newUser))
                        connectedUsers.add(new User(newUser));
                });
                // Enviamos confirmación, pero no lo mostramos en el chat.
                try {
                    sendMessage("confirmación: " + userName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return; // Evitamos que se muestre el mensaje de conexión.
            }
        }
        else if (message.startsWith("confirmación:")) {
            // Procesamos la confirmación, pero NO la mostramos en el chat.
            String confirmUser = message.substring("confirmación:".length()).trim();
            if (!confirmUser.equals(userName)) {
                Platform.runLater(() -> {
                    if (!containsUser(confirmUser))
                        connectedUsers.add(new User(confirmUser));
                });
            }
            return;  // Salimos sin llamar a onMessageReceived para no mostrarlo.
        }
        else if (message.endsWith("se ha desconectado.")) {
            String disconnectedUser = message.split(" ")[0].trim();
            Platform.runLater(() -> connectedUsers.removeIf(u -> u.getName().equals(disconnectedUser)));
            return; // Evitamos mostrar el mensaje de desconexión.
        }

        // Solo mostramos el mensaje en el chat si no es de sistema.
        Platform.runLater(() -> onMessageReceived.accept(message));
    }

    /**
     * Verifica si la lista de usuarios ya contiene un usuario con el nombre especificado.
     */
    private boolean containsUser(String name) {
        return connectedUsers.stream().anyMatch(u -> u.getName().equals(name));
    }

    /**
     * Actualiza la lista de usuarios conectados a partir de un mensaje
     * con formato "Usuarios conectados: [usuario1, usuario2, ...]".
     */
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

    /**
     * Devuelve la lista observable de usuarios conectados.
     */
    public ObservableList<User> getConnectedUsers() {
        return connectedUsers;
    }

    /**
     * Cierra el socket UDP para finalizar la comunicación.
     */
    public void close() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }

    /**
     * Clase para representar un usuario conectado.
     */
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

    /**
     * Celda personalizada para el ListView que muestra la lista de usuarios.
     */
    public static class UserCell extends ListCell<User> {
        private final HBox hbox = new HBox(5);
        private final Label nameLabel = new Label();
        private final Circle statusCircle = new Circle(5);
        private final Label statusLabel = new Label("En línea");

        public UserCell() {
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
