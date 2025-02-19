import java.net.*;
import java.io.*;

public class ChatWhatsapp {
    private static final int PORT = 9876;
    // Cambia la dirección de broadcast según tu red (ej.: "192.168.182.255" o "255.255.255.255")
    private static final String BROADCAST_IP = "192.168.182.255";

    public static void main(String[] args) {
        try {
            // Creamos el socket sin asignar puerto, habilitamos SO_REUSEADDR y lo vinculamos al puerto deseado.
            DatagramSocket socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);
            System.out.println("Chat UDP iniciado en el puerto " + PORT);

            // Obtener la dirección IP local
            String localIp = InetAddress.getLocalHost().getHostAddress();

            // Enviar un mensaje de conexión a todos los usuarios
            String welcomeMessage = "Usuario con IP " + localIp + " se ha conectado.";
            byte[] welcomeData = welcomeMessage.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_IP);
            DatagramPacket welcomePacket = new DatagramPacket(welcomeData, welcomeData.length, broadcastAddress, PORT);
            socket.send(welcomePacket);

            // Hilo para recibir mensajes
            Thread receptor = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (true) {
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        InetAddress senderAddress = packet.getAddress();

                        // Evitar mostrar mensajes propios
                        if (!senderAddress.equals(InetAddress.getLocalHost())) {
                            System.out.println("Mensaje de " + senderAddress.getHostAddress() + ": " + message);
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                byte[] data = line.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}