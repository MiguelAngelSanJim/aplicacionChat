import java.net.*;
import java.io.*;
import java.util.*;

public class ChatWhatsapp {
    private static final int PORT = 9876;
    private static final String BROADCAST_IP = "192.168.182.255";
    private static final String CONFIG_FILE = "config.txt";
    private static String userName;
    private static BufferedReader reader;
    private static Set<String> usersConnected = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) { 
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            userName = readUserName();

            // Al conectar, añadir a la lista de usuarios
            usersConnected.add(userName);

            DatagramSocket socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);

            // Enviar un mensaje de conexión a todos los usuarios
            sendMessage(socket, userName + " se ha conectado.");
            displayUserList();

            // Hilo para recibir mensajes
            Thread receptor = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (true) {
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        processMessage(socket, message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receptor.start();

            // Bucle para enviar mensajes
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("salir")) {
                    sendMessage(socket, userName + " se ha desconectado.");
                    break; // Salir del bucle
                } else {
                    sendMessage(socket, userName + ": " + line);
                }
            }

            socket.close();
            System.out.println("Desconectado del chat.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processMessage(DatagramSocket socket, String message) {
        if (message.endsWith("se ha conectado.")) {
            String newUser = message.split(" ")[0];
            if (usersConnected.add(newUser)) {
                System.out.println(newUser + " se ha conectado.");
                displayUserList();
                // Responder con la lista de usuarios conectados
                sendMessage(socket, getUsersListMessage());
            }
        } else if (message.endsWith("se ha desconectado.")) {
            String disconnectedUser = message.split(" ")[0];
            if (usersConnected.remove(disconnectedUser)) {
                System.out.println(disconnectedUser + " se ha desconectado.");
                displayUserList();
            }
        } else {
            System.out.println(message); // Mostrar otros mensajes
        }
    }

    private static String readUserName() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                return br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.out.print("Introduce tu nombre: ");
                String name = reader.readLine();
                saveUserName(name);
                return name;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "Usuario"; // Nombre por defecto si falla
    }

    private static void saveUserName(String name) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(DatagramSocket socket, String message) {
        try {
            byte[] data = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_IP);
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getUsersListMessage() {
        StringBuilder userList = new StringBuilder("Usuarios conectados: [");
        synchronized (usersConnected) {
            for (String user : usersConnected) {
                userList.append(user).append(", ");
            }
        }
        if (userList.length() > 0) {
            userList.setLength(userList.length() - 2); // Eliminar la última coma
        }
        userList.append("]");
        return userList.toString();
    }

    private static void displayUserList() {
        System.out.println(getUsersListMessage());
    }
}
