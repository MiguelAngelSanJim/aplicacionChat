package org.example.demo4;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.*;
import java.util.Optional;

/**
 * Controlador del chat que gestiona la interacción del usuario con la interfaz gráfica.
 * Maneja el envío y la recepción de mensajes, la gestión de usuarios conectados y la
 * configuración del nombre de usuario.
 */
public class ChatController {

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;
    /**
     * Lista de usuarios conectados.
     */
    @FXML
    private ListView<ChatModel.User> usersListView;

    private ChatModel model;
    private String userName;
    private static final String CONFIG_FILE = "config.txt";

    /**
     * Inicializa el controlador, configurando la conexión del usuario y enlazando
     * los componentes gráficos con la lógica del chat.
     */
    @FXML
    public void initialize() {
        userName = readUserName();
        chatArea.appendText("Bienvenido, " + userName + "!\n");

        try {
            model = new ChatModel(userName);
        } catch (Exception e) {
            e.printStackTrace();
            chatArea.appendText("Error iniciando el modelo: " + e.getMessage() + "\n");
            return;
        }

        usersListView.setItems(model.getConnectedUsers());
        usersListView.setCellFactory(lv -> new ChatModel.UserCell());

        try {
            String welcomeMessage = userName + " se ha conectado.";
            model.sendMessage(welcomeMessage);
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje de bienvenida: " + e.getMessage() + "\n");
        }

        model.startReceiver(message -> Platform.runLater(() -> chatArea.appendText(message + "\n")));

        sendButton.setOnAction(event -> sendMessage());
        inputField.setOnAction(event -> sendMessage());
    }

    /**
     * Envía un mensaje al chat si el campo de entrada no está vacío.
     */
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

    /**
     * Lee el nombre del usuario desde un archivo de configuración o lo solicita mediante un diálogo.
     *
     * @return el nombre de usuario.
     */
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

        Image icono = new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png"));
        ImageView imageView = new ImageView(icono);
        imageView.setFitWidth(48);  // Ajusta el tamaño si es necesario
        imageView.setFitHeight(48);


        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(imageView);

        dialog.setTitle("chApp");
        dialog.setHeaderText("Introduce tu nombre de usuario:");
        dialog.setContentText("Nombre:");
        // Después de crear el Alert, obtenemos su Stage a través del DialogPane
        Stage alertStage = (Stage) dialog.getDialogPane().getScene().getWindow();

        // Limpiamos los iconos existentes (opcional) y añadimos nuestro icono personalizado
        alertStage.getIcons().clear();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String name = result.get().trim();
            saveUserName(name);
            return name;
        }
        return "Usuario";
    }

    /**
     * Guarda el nombre del usuario en un archivo de configuración.
     *
     * @param name el nombre de usuario a guardar.
     */
    private void saveUserName(String name) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Maneja el cierre de la aplicación, enviando un mensaje de desconexión y liberando recursos.
     */
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
