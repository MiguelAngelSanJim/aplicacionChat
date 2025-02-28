package org.example.demo4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;


public class PrivateChatTest extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicia la ventana de prueba
        ChatModel fakeModel = new ChatModel("Usuario1") {
            @Override
            public void sendMessage(String message) {
                System.out.println("MENSAJE ENVIADO SIMULADO: " + message);
            }
        };

        FXMLLoader loader = new FXMLLoader(getClass().getResource("PrivateChat.fxml"));
        Parent root = loader.load();
        PrivateChatController chatController = loader.getController();
        chatController.initData("Usuario1", "Usuario2", fakeModel);

        System.out.println("Probando recepción de mensajes...");
        chatController.receiveMessage("Usuario2", "Hola, ¿cómo estás?");
        chatController.receiveMessage("Usuario1", "¡Bien, gracias!");
    }
}
