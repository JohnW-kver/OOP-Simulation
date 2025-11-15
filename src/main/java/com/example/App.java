package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.scene = new Scene(load("physics"), 800, 900);
        primaryStage.setTitle("Physics Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Parent load(String fxmlName) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
        return fxmlLoader.load();
    }

    static void setRoot(String fxmlName) throws IOException {
        scene.setRoot(load(fxmlName));
    }

    public static void main(String[] args) {
        launch();
    }
}
