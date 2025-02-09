package restaurant.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;

public class MainController {
    @FXML private Button startButton;
    @FXML private Slider speedSlider;

    public void initialize() {
        startButton.setOnAction(event -> startSimulation());
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> adjustSpeed(newVal.doubleValue()));
    }

    private void startSimulation() {
        System.out.println("Simulation Started");
    }

    private void adjustSpeed(double speed) {
        System.out.println("Speed adjusted to: " + speed);
    }
}
