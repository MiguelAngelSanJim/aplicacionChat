package org.example.demo4;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChatController {

    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button vibrateButton;
    @FXML private ListView<ChatModel.User> usersListView;
    @FXML private Text onlineUser;

    private ChatModel model;
    private String userName;
    private static final String CONFIG_FILE = "config.txt";

    private Map<String, PrivateChatController> openPrivateChats = new HashMap<>();

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
        usersListView.setCellFactory(lv -> {
            ChatModel.UserCell cell = new ChatModel.UserCell();
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    ChatModel.User selectedUser = cell.getItem();
                    if (selectedUser != null && !selectedUser.getName().equals(userName)) {
                        try {
                            String request = "privateRequest:" + userName + ":" + selectedUser.getName();
                            model.sendMessage(request);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            return cell;
        });

        try {
            String welcomeMessage = userName + " se ha conectado.";
            model.sendMessage(welcomeMessage);
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error enviando mensaje de bienvenida: " + e.getMessage() + "\n");
        }

        model.startReceiver(message -> Platform.runLater(() -> {
            if (message.startsWith("vibrate:")) {
                vibrateScreen();
            } else if (message.startsWith("privateRequest:")) {
                handlePrivateRequest(message);
            } else if (message.startsWith("privateAccept:")) {
                handlePrivateAccept(message);
            } else if (message.startsWith("privateReject:")) {
                handlePrivateReject(message);
            } else if (message.startsWith("privateMessage:")) {
                handlePrivateMessage(message);
            } else {
                chatArea.appendText(message + "\n");
            }
        }));

        sendButton.setOnAction(event -> sendMessage());
        inputField.setOnAction(event -> sendMessage());
        vibrateButton.setOnAction(event -> sendVibrateMessage());
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

    private void sendVibrateMessage() {
        try {
            model.sendMessage("vibrate:" + userName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void vibrateScreen() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), chatArea);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
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

    private void openPrivateChatWindow(String myName, String otherUser) {
        if (openPrivateChats.containsKey(otherUser)) {
            PrivateChatController existingController = openPrivateChats.get(otherUser);
            Stage existingStage = existingController.getStage();
            if (existingStage != null) {
                existingStage.toFront();
                existingStage.requestFocus();
            }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chatviewprivate.fxml"));
            Parent root = loader.load();

            PrivateChatController controller = loader.getController();
            controller.initData(myName, otherUser, model);

            openPrivateChats.put(otherUser, controller);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("chApp");

            // Cargar el CSS
            String css = getClass().getResource("privateChatLight.css").toExternalForm();
            stage.getScene().getStylesheets().add(css);

            controller.setStage(stage);

            stage.setOnCloseRequest(e -> {
                openPrivateChats.remove(otherUser);
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    // Métodos auxiliares para manejar mensajes privados
    private void handlePrivateRequest(String message) {
        String[] parts = message.split(":");
        if (parts.length >= 3 && parts[2].equals(userName)) {
            String fromUser = parts[1];
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            Image icono = new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png"));
            ImageView imageView = new ImageView(icono);
            imageView.setFitWidth(48);
            imageView.setFitHeight(48);
            alert.setGraphic(imageView);
            alert.setTitle("chApp");
            alert.setHeaderText("Solicitud de chat privado.");
            alert.setContentText("¿Aceptas la invitación de " + fromUser + "?");
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().clear();
            alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    model.sendMessage("privateAccept:" + userName + ":" + fromUser);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                openPrivateChatWindow(userName, fromUser);
            } else {
                try {
                    model.sendMessage("privateReject:" + userName + ":" + fromUser);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlePrivateAccept(String message) {
        String[] parts = message.split(":");
        if (parts.length >= 3 && parts[2].equals(userName)) {
            String otherUser = parts[1];
            openPrivateChatWindow(userName, otherUser);
        }
    }

    private void handlePrivateReject(String message) {
        String[] parts = message.split(":");
        if (parts.length >= 3 && parts[2].equals(userName)) {
            String otherUser = parts[1];
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            Image icono = new Image(getClass().getResourceAsStream("/org/example/demo4/rechazar.png"));
            ImageView imageView = new ImageView(icono);
            imageView.setFitWidth(48);
            imageView.setFitHeight(48);
            alert.setGraphic(imageView);
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().clear();
            alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));
            alert.setTitle("Solicitud rechazada");
            alert.setHeaderText(null);
            alert.setContentText(otherUser + " ha rechazado tu solicitud de chat privado.");
            alert.showAndWait();
        }
    }

    private void handlePrivateMessage(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length == 4) {
            String sender = parts[1];
            String recipient = parts[2];
            String content = parts[3];

            if (recipient.equals(userName) || sender.equals(userName)) {
                String otherUser = sender.equals(userName) ? recipient : sender;
                PrivateChatController privateChat = openPrivateChats.get(otherUser);

                if (privateChat == null) {
                    openPrivateChatWindow(userName, otherUser);
                    privateChat = openPrivateChats.get(otherUser);
                }

                if (privateChat != null) {
                    String senderName = sender.equals(userName) ? "Yo" : sender;
                    privateChat.receiveMessage(senderName, content);
                }
            }
        }
    }
}
