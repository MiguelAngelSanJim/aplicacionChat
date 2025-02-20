package org.example.demo4;

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
        // Añade el usuario local a la lista de conectados.
        connectedUsers.add(userName);

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        socket.setBroadcast(true);
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
                        // Si el socket está cerrado, salimos del bucle sin imprimir la traza.
                        if (socket.isClosed()) {
                            return;
                        } else {
                            throw se;
                        }
                    }
                    String message = new String(packet.getData(), 0, packet.getLength());

                    // Procesar el mensaje recibido:
                    if (message.startsWith(userName)) {
                        onMessageReceived.accept("mensaje (eco): " + message);
                    } else {
                        if (message.endsWith("se ha conectado.")) {
                            String[] parts = message.split(" ");
                            if (parts.length > 0) {
                                String newUser = parts[0];
                                if (!connectedUsers.contains(newUser)) {
                                    connectedUsers.add(newUser);
                                }
                            }
                        }
                        onMessageReceived.accept(message);
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {  // Solo imprime si el error no es por el cierre normal.
                    e.printStackTrace();
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
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
