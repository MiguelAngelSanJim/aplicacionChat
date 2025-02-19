import java.net.*;
import java.io.*;

public class ChatWhatsapp {
    private static final int PORT = 9876;
    private static final String BROADCAST_IP = "192.168.182.255";
    private static final String CONFIG_FILE = "config.txt";
    private static String userName;
    private static BufferedReader reader;

    public static void main(String[] args) {
        try {
            // Inicializar BufferedReader para lectura de la entrada estándar
            reader = new BufferedReader(new InputStreamReader(System.in));

            // Leer el nombre del usuario desde el archivo de configuración
            userName = readUserName();
            System.out.println("Bienvenido, " + userName + "!");

            DatagramSocket socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);
            System.out.println("Chat UDP iniciado en el puerto " + PORT);

            // Enviar un mensaje de conexión a todos los usuarios
            String welcomeMessage = userName + " se ha conectado.";
            sendMessage(socket, welcomeMessage);

            // Hilo para recibir mensajes
            Thread receptor = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (true) {
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());

                        // Evitar mostrar mensajes propios
                        if (!message.startsWith(userName)) {
                            System.out.println("Mensaje de " + message);
                        } else {
                            System.out.println("Mensaje (eco): " + message);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receptor.start();

            // Bucle para enviar mensajes
            String line;
            while ((line = reader.readLine()) != null) {
                sendMessage(socket, userName + ": " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            // Si no existe, pide un nombre y lo guarda en el archivo
            try {
                System.out.print("Introduce tu nombre: ");
                String name = reader.readLine(); // Usa el BufferedReader global
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

    private static void sendMessage(DatagramSocket socket, String message) throws IOException {
        byte[] data = message.getBytes();
        InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_IP);
        DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
        socket.send(packet);
    }
}
