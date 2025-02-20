package org.example.demo4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private ChatController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Chatview.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(getClass().getResource("chat.css").toExternalForm());
        primaryStage.setTitle("Chat UDP JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Al cerrar la ventana se liberan los recursos (cierra el socket)
        primaryStage.setOnCloseRequest(event -> controller.shutdown());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
