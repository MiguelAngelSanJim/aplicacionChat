package org.example.demo4;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.Optional;

/**
 * Controlador del chat que gestiona la interacción del usuario con la interfaz gráfica.
 * Maneja el envío y la recepción de mensajes, la gestión de usuarios conectados y la
 * configuración del nombre de usuario.
 */
public class ChatController {

    public Text onlineUser;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button vibrateButton; // Nuevo botón para vibrar
    @FXML private ListView<ChatModel.User> usersListView;

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

        // Vincula la lista de usuarios conectados con el ListView y asigna la celda personalizada.
        usersListView.setItems(model.getConnectedUsers());
        usersListView.setCellFactory(lv -> new ChatModel.UserCell());

        try {
            String welcomeMessage = userName + " se ha conectado.";
            model.sendMessage(welcomeMessage);
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje de bienvenida: " + e.getMessage() + "\n");
        }

        // Inicia el receptor de mensajes. Si se recibe un mensaje que comience por "vibrate:",
        // se activa el efecto de vibración en el área de chat.
        model.startReceiver(message -> Platform.runLater(() -> {
            if (message.startsWith("vibrate:")) {
                vibrateScreen();
            } else {
                chatArea.appendText(message + "\n");
            }
        }));

        sendButton.setOnAction(event -> sendMessage());
        inputField.setOnAction(event -> sendMessage());
        vibrateButton.setOnAction(event -> sendVibrateMessage());
    }

    /**
     * Envía el contenido del campo de entrada como mensaje.
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
     * Envía un mensaje especial para activar la vibración en otros usuarios.
     */
    private void sendVibrateMessage() {
        try {
            model.sendMessage("vibrate:" + userName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aplica un efecto de vibración (shake) al área de chat.
     */
    private void vibrateScreen() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), chatArea);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
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
    imageView.setFitWidth(48);
    imageView.setFitHeight(48);

    TextInputDialog dialog = new TextInputDialog();
    dialog.setGraphic(imageView);
    dialog.setTitle("chApp");
    dialog.setHeaderText("Introduce tu nombre de usuario:");
    dialog.setContentText("Nombre:");

    Stage alertStage = (Stage) dialog.getDialogPane().getScene().getWindow();
    alertStage.getIcons().clear();
    alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));

    Optional<String> result;
    do {
        result = dialog.showAndWait();
        if (result.isPresent() && result.get().trim().isEmpty()) {
            // Muestra un mensaje de advertencia si el campo está vacío
            dialog.setContentText("El nombre no puede estar vacío.\nPor favor, introduce un nombre:\n");
        }
    } while (result.isPresent() && result.get().trim().isEmpty());

    if (result.isPresent()) {
        String name = result.get().trim();
        saveUserName(name);
        return name;
    } else {
        // Cierra la aplicación si el usuario cancela
        Platform.exit();
        System.exit(0); // Opcional, para asegurarte de que la aplicación se cierra
    }

    return null;
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
