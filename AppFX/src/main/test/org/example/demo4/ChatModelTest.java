package org.example.demo4;

import javafx.application.Platform;
import org.example.demo4.ChatModel;

import java.io.IOException;

public class ChatModelTest {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                System.out.println("Iniciando ChatModel para Usuario1...");
                ChatModel chat1 = new ChatModel("Usuario1");
                System.out.println("ChatModel para Usuario1 creado con éxito.");

                System.out.println("Iniciando ChatModel para Usuario2...");
                ChatModel chat2 = new ChatModel("Usuario2");
                System.out.println("ChatModel para Usuario2 creado con éxito.");

                chat2.startReceiver(message -> {
                    System.out.println("Mensaje recibido en Usuario2: " + message);
                });

                Thread.sleep(1000); // Espera para que el receptor se inicie

                System.out.println("Enviando mensaje desde Usuario1...");
                chat1.sendMessage("Hola, prueba de conexión.");

                Thread.sleep(3000); // Espera para permitir la recepción

                chat1.close();
                chat2.close();
                System.out.println("Prueba finalizada correctamente.");

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
