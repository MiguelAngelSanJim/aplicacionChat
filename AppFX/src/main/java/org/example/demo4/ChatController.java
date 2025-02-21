package org.example.demo4;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import java.io.*;
import java.util.Optional;

public class ChatController {

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;
    // Actualizamos el tipo de la ListView para que use ChatModel. User
    @FXML
    private ListView<ChatModel.User> usersListView;

    private ChatModel model;
    private String userName;
    private static final String CONFIG_FILE = "config.txt";

    @FXML
    public void initialize() {
        // Lee el nombre del usuario desde el archivo o lo solicita mediante diálogo.
        userName = readUserName();
        chatArea.appendText("Bienvenido, " + userName + "!\n");

        try {
            model = new ChatModel(userName);
        } catch (Exception e) {
            e.printStackTrace();
            chatArea.appendText("Error iniciando el modelo: " + e.getMessage() + "\n");
            return;
        }

        // Vincula la lista de usuarios conectados con el ListView y establece la celda personalizada.
        usersListView.setItems(model.getConnectedUsers());
        usersListView.setCellFactory(lv -> new ChatModel.UserCell());

        // Envía un mensaje de conexión.
        try {
            String welcomeMessage = userName + " se ha conectado.";
            model.sendMessage(welcomeMessage);
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje de bienvenida: " + e.getMessage() + "\n");
        }

        // Inicia el receptor UDP; cada mensaje recibido se añade al chat.
        model.startReceiver(message -> Platform.runLater(() -> chatArea.appendText(message + "\n")));

        sendButton.setOnAction(event -> sendMessage());
        inputField.setOnAction(event -> sendMessage());
    }

    private void sendMessage() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String message = userName + ": " + text;
        try {
            model.sendMessage(message);
            inputField.clear();
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje: " + e.getMessage() + "\n");
        }
    }

    private String readUserName() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String name = br.readLine();
                if (name != null && !name.trim().isEmpty()) {
                    return name.trim();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Si no existe o no hay un nombre válido, se solicita mediante un diálogo.
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nombre de usuario");
        dialog.setHeaderText("Introduce tu nombre de usuario:");
        dialog.setContentText("Nombre:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String name = result.get().trim();
            saveUserName(name);
            return name;
        }
        return "Usuario";
    }

    private void saveUserName(String name) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Al cerrar la aplicación se envía el mensaje de desconexión y se libera el socket.
    public void shutdown() {
        if (model != null) {
            try {
                model.sendMessage(userName + " se ha desconectado.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            model.close();
        }
    }
}
