package com.example.mdreader;

import com.example.mdreader.controller.ReaderController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ReaderController controller = new ReaderController(stage);
        Scene scene = new Scene(
                controller.createContent(),
                controller.preferences().windowWidth(),
                controller.preferences().windowHeight()
        );

        stage.setTitle("md-reader");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
