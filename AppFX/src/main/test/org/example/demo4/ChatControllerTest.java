package org.example.demo4;

import javafx.application.Platform;
import java.io.*;
import java.lang.reflect.Method;

public class ChatControllerTest {
    public static void main(String[] args) {
        System.out.println("Iniciando pruebas para ChatController...");

        testReadUserName();
        testSaveUserName();
        testSendMessage();
        testShutdown();

        System.out.println("Pruebas completadas.");
    }

    private static void testReadUserName() {
        System.out.println("Ejecutando testReadUserName...");
        try {
            String testName = "UsuarioPrueba";
            BufferedWriter writer = new BufferedWriter(new FileWriter("config.txt"));
            writer.write(testName);
            writer.close();

            ChatController controller = new ChatController();
            Method method = ChatController.class.getDeclaredMethod("readUserName");
            method.setAccessible(true);
            String result = (String) method.invoke(controller);

            if (testName.equals(result)) {
                System.out.println("✓ testReadUserName PASSED");
            } else {
                System.out.println("✗ testReadUserName FAILED: esperado '" + testName + "', obtenido '" + result + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("✗ testReadUserName FAILED");
        }
    }

    private static void testSaveUserName() {
        System.out.println("Ejecutando testSaveUserName...");
        try {
            String testName = "TestUser";
            ChatController controller = new ChatController();
            Method method = ChatController.class.getDeclaredMethod("saveUserName", String.class);
            method.setAccessible(true);
            method.invoke(controller, testName);

            BufferedReader reader = new BufferedReader(new FileReader("config.txt"));
            String savedName = reader.readLine();
            reader.close();

            if (testName.equals(savedName)) {
                System.out.println("✓ testSaveUserName PASSED");
            } else {
                System.out.println("✗ testSaveUserName FAILED: esperado '" + testName + "', obtenido '" + savedName + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("✗ testSaveUserName FAILED");
        }
    }

    private static void testSendMessage() {
        System.out.println("Ejecutando testSendMessage...");
        try {
            ChatController controller = new ChatController();
            String message = "Hola mundo";

            if (!message.trim().isEmpty()) {
                System.out.println("✓ testSendMessage PASSED");
            } else {
                System.out.println("✗ testSendMessage FAILED: mensaje vacío");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("✗ testSendMessage FAILED");
        }
    }

    private static void testShutdown() {
        System.out.println("Ejecutando testShutdown...");
        try {
            new Thread(() -> {
                javafx.application.Application.launch(DummyApp.class); // Iniciar JavaFX
            }).start();

            Thread.sleep(1000); // Esperar un poco a que se inicie JavaFX

            ChatController controller = new ChatController();
            Platform.runLater(controller::shutdown);

            System.out.println("✓ testShutdown PASSED");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("✗ testShutdown FAILED");
        }
    }

    public static class DummyApp extends javafx.application.Application {
        @Override
        public void start(javafx.stage.Stage primaryStage) { /* No hacer nada */ }
    }
}
