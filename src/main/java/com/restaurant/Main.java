package com.restaurant;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/com.restaurant/views/MainView.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
        Label titleLabel = new Label("Restaurant Simulation");
        Button startButton = new Button("Start Simulation");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

