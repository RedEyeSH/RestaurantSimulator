package View;

import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import Controller.RestaurantController;

public class RestaurantView {

    private VBox queueBox, orderingBox, waitingBox, kitchenBox, servedBox, payBox, leftBox;
    private Label queueLabel, orderingLabel, waitingLabel, kitchenLabel, servedLabel, payLabel, leftLabel;
    private Label queueContent, orderingContent, waitingContent, kitchenContent, servedContent, payContent, leftContent;
    private ComboBox<Integer> machineSelector;
    private ComboBox<Integer> chefSelector;

    private RestaurantController controller;

    // Modify the constructor to accept the controller as a parameter (it's now null when created)
    public RestaurantView(Stage primaryStage) {
        queueLabel = new Label("Queue (0):");
        orderingLabel = new Label("Ordering (0):");
        waitingLabel = new Label("Waiting (0):");
        kitchenLabel = new Label("Kitchen (0):");
        servedLabel = new Label("Served (0):");
        payLabel = new Label("Pay (0):");
        leftLabel = new Label("Left (0):");

        queueContent = new Label();
        orderingContent = new Label();
        waitingContent = new Label();
        kitchenContent = new Label();
        servedContent = new Label();
        payContent = new Label();
        leftContent = new Label();

        queueBox = new VBox(10, queueLabel, queueContent);
        orderingBox = new VBox(10, orderingLabel, orderingContent);
        waitingBox = new VBox(10, waitingLabel, waitingContent);
        kitchenBox = new VBox(10, kitchenLabel, kitchenContent);
        servedBox = new VBox(10, servedLabel, servedContent);
        payBox = new VBox(10, payLabel, payContent);
        leftBox = new VBox(10, leftLabel, leftContent);

        HBox mainLayout = new HBox(50, queueBox, orderingBox, waitingBox, kitchenBox, servedBox, payBox, leftBox);

        // Create ComboBoxes for selecting machines and chefs
        machineSelector = new ComboBox<>();
        chefSelector = new ComboBox<>();

        // Populate ComboBoxes with numbers from 1 to 5
        for (int i = 1; i <= 10; i++) {
            machineSelector.getItems().add(i);
            chefSelector.getItems().add(i);
        }
        machineSelector.setValue(1);
        chefSelector.setValue(1);

        VBox controlBox = new VBox(10,
                new Label("🔧 Select Ordering Machines:"), machineSelector,
                new Label("👨‍🍳 Select Number of Chefs:"), chefSelector
        );

        VBox root = new VBox(20, mainLayout, controlBox);

        Scene scene = new Scene(root, 1150, 350);
        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);

        // Add listeners to update the controller when the selection changes
        machineSelector.setOnAction(e -> {
            if (controller != null) {
                int selectedMachines = machineSelector.getValue();
                controller.updateAvailableMachines(selectedMachines);
            }
        });

        chefSelector.setOnAction(e -> {
            if (controller != null) {
                int selectedChefs = chefSelector.getValue();
                controller.updateAvailableChefs(selectedChefs);
            }
        });
    }

    // Add the setController() method
    public void setController(RestaurantController controller) {
        this.controller = controller;
    }

    public void show(Stage primaryStage) {
        primaryStage.show();
    }

    // Getter methods for the controllers
    public ComboBox<Integer> getMachineSelector() {
        return machineSelector;
    }

    public ComboBox<Integer> getChefSelector() {
        return chefSelector;
    }

    public Label getQueueLabel() {
        return queueLabel;
    }

    public Label getOrderingLabel() {
        return orderingLabel;
    }

    public Label getWaitingLabel() {
        return waitingLabel;
    }

    public Label getKitchenLabel() {
        return kitchenLabel;
    }

    public Label getServedLabel() {
        return servedLabel;
    }

    public Label getPayLabel() {
        return payLabel;
    }

    public Label getLeftLabel() {
        return leftLabel;
    }

    public Label getQueueContent() {
        return queueContent;
    }

    public Label getOrderingContent() {
        return orderingContent;
    }

    public Label getWaitingContent() {
        return waitingContent;
    }

    public Label getKitchenContent() {
        return kitchenContent;
    }

    public Label getServedContent() {
        return servedContent;
    }

    public Label getPayContent() {
        return payContent;
    }

    public Label getLeftContent() {
        return leftContent;
    }
}

