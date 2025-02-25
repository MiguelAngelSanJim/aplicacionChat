package org.example.demo4;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador para la ventana de chat privado.
 */
public class PrivateChatController {
    @FXML
    private Text chatTitle;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;

    private String myName;
    private String otherUser;
    private ChatModel model;

    // Referencia a la Stage para poder traerla al frente o cerrarla
    private Stage stage;

    /**
     * Método de inicialización de datos. Se llama desde ChatController al cargar este FXML.
     */
    public void initData(String myName, String otherUser, ChatModel model) {
        this.myName = myName;
        this.otherUser = otherUser;
        this.model = model;
        chatTitle.setText("Chat privado con " + otherUser);

        // Acción para enviar mensaje
        sendButton.setOnAction(event -> sendPrivateMessage());
        inputField.setOnAction(event -> sendPrivateMessage());
    }

    /**
     * Envía un mensaje privado al otro usuario usando el mismo modelo (UDP broadcast).
     * Se envía con el prefijo "privateMessage:" para que los demás lo ignoren (o no lo muestren).
     */
    private void sendPrivateMessage() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        // Formato: privateMessage:emisor:destinatario:contenido
        String message = "privateMessage:" + myName + ":" + otherUser + ":" + text;
        try {
            model.sendMessage(message);
            // Mostrarlo en nuestra ventana
            chatArea.appendText("Yo: " + text + "\n");
            inputField.clear();
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje: " + e.getMessage() + "\n");
        }
    }

    /**
     * Método para recibir un mensaje privado y mostrarlo en el área de chat.
     * Lo llama ChatController cuando detecta un privateMessage para este chat.
     */
    public void receiveMessage(String sender, String content) {
        Platform.runLater(() -> {
            chatArea.appendText(sender + ": " + content + "\n");
        });
    }

    /**
     * Permite a ChatController establecer la Stage asociada a esta ventana.
     */
    public void setStage(Stage stage) {
        this.stage = stage;

        stage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));
    }

    /**
     * Devuelve la Stage de este chat privado (para poder traerla al frente, cerrarla, etc.).
     */
    public Stage getStage() {
        return stage;
    }
}
