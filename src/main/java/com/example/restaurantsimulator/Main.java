package com.example.restaurantsimulator;

import javafx.application.Application;
import javafx.stage.Stage;
import View.RestaurantView;
import Controller.RestaurantController;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize the View first
        RestaurantView view = new RestaurantView(primaryStage);  // No controller passed yet

        // Now, initialize the Controller and pass it to the view
        RestaurantController controller = new RestaurantController(view);

        // Set the controller in the view after both are initialized
        view.setController(controller);  // Set the controller

        // Start the simulation after the controller has been set
        controller.startSimulation();

        // Show the JavaFX window
        view.show(primaryStage);
    }

    public static void main(String[] args) {
        // Launch the JavaFX application
        launch(args);
    }
}



