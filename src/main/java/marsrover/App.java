package marsrover;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Label title = new Label("Mars Rover");
        title.setStyle("-fx-font-size: 72px; -fx-text-fill: #f4e1d2; -fx-font-weight: bold;");

        StackPane root = new StackPane(title);
        root.setStyle("-fx-background-color: #3b1a12;");

        stage.setTitle("Mars Rover");
        stage.setScene(new Scene(root, 1920, 1080));
        stage.setFullScreen(true);
        stage.show();
    }
}
