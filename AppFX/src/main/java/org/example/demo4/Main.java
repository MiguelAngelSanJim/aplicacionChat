package org.example.demo4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicación que extiende {@link Application} y maneja la inicialización
 * de la interfaz gráfica de usuario basada en JavaFX.
 * <p>
 * Carga la vista definida en "Chatview.fxml", aplica la hoja de estilos "chat.css"
 * y establece el controlador {@link ChatController}. Además, gestiona el cierre
 * de la ventana asegurándose de liberar los recursos utilizados.
 * </p>
 *
 * @author Rubén Matamoros
 * @author Miguel Ángel Sánchez
 */
public class Main extends Application {

    /**
     * Controlador de la vista del chat.
     */
    private ChatController controller;

    /**
     * Método de inicio de la aplicación JavaFX.
     *
     * @param primaryStage la ventana principal de la aplicación.
     * @throws Exception si ocurre un error al cargar la vista FXML.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Chatview.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 600, 400);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/demo4/logo.png")));
        scene.getStylesheets().add(getClass().getResource("chat.css").toExternalForm());
        primaryStage.setTitle("chApp");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Al cerrar la ventana, se liberan los recursos (cierra el socket).
        primaryStage.setOnCloseRequest(event -> controller.shutdown());
    }

    /**
     * Método principal de la aplicación. Inicia la ejecución de JavaFX.
     *
     * @param args argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
