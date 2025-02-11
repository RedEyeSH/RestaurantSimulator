package com.example.restaurantsimulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class demo extends Application {
    private static final int MAX_MACHINES = 3;
    private Queue<Integer> queue = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
    private Queue<String> kitchenList = new LinkedList<>();
    private Queue<String> servedList = new LinkedList<>();
    private Queue<String> leftList = new LinkedList<>();

    private Map<Integer, Long> customerArrivalTimes = new ConcurrentHashMap<>();
    private AtomicInteger customerID = new AtomicInteger(1);

    private int availableMachines = 1;
    private int activeOrders = 0;

    private Label queueLabel = new Label("Queue (0):");
    private Label orderingLabel = new Label("Ordering (0):");
    private Label waitingLabel = new Label("Waiting (0):");
    private Label kitchenLabel = new Label("Kitchen (0):");
    private Label servedLabel = new Label("Served (0):");
    private Label leftLabel = new Label("Left (0):");

    private Label queueContent = new Label();
    private Label orderingContent = new Label();
    private Label waitingContent = new Label();
    private Label kitchenContent = new Label();
    private Label servedContent = new Label();
    private Label leftContent = new Label();

    private ComboBox<Integer> machineSelector = new ComboBox<>();
    private Random random = new Random();
    private String[] foodOptions = {"Burger", "Pizza", "Pasta", "Sushi", "Salad"};

    @Override
    public void start(Stage primaryStage) {
        VBox queueBox = new VBox(10, queueLabel, queueContent);
        VBox orderingBox = new VBox(10, orderingLabel, orderingContent);
        VBox waitingBox = new VBox(10, waitingLabel, waitingContent);
        VBox kitchenBox = new VBox(10, kitchenLabel, kitchenContent);
        VBox servedBox = new VBox(10, servedLabel, servedContent);
        VBox leftBox = new VBox(10, leftLabel, leftContent);

        HBox mainLayout = new HBox(50, queueBox, orderingBox, waitingBox, kitchenBox, servedBox, leftBox);

        // Dropdown for selecting the number of ordering machines (moved to bottom)
        machineSelector.getItems().addAll(1, 2, 3);
        machineSelector.setValue(1);
        machineSelector.setOnAction(e -> updateOrderingMachines(machineSelector.getValue()));

        VBox controlBox = new VBox(10, new Label("🔧 Select Ordering Machines:"), machineSelector);
        VBox root = new VBox(20, mainLayout, controlBox);

        Scene scene = new Scene(root, 1150, 350);

        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        startSimulation();
    }

    private void startSimulation() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    int id = customerID.getAndIncrement();
                    long startTime = System.currentTimeMillis();
                    Platform.runLater(() -> addCustomerToQueue(id, startTime));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addCustomerToQueue(int id, long startTime) {
        customerArrivalTimes.put(id, startTime);
        queue.add(id);
        updateQueueLabel();
        processQueue();
    }

    private synchronized void processQueue() {
        // Only proceed if there are available machines and the queue is not empty
        while (activeOrders < availableMachines && !queue.isEmpty()) {
            int id = queue.poll(); // Retrieve customer from the queue
            activeOrders++; // Increment the active orders count
            orderingLabel.setText("Ordering (" + activeOrders + "):");
            orderingContent.setText(orderingContent.getText() + "Customer " + id + " is ordering\n");
            updateQueueLabel();

            // Start a new thread to simulate the ordering process
            new Thread(() -> {
                try {
                    Thread.sleep(7000); // Simulate ordering time
                    String food = foodOptions[random.nextInt(foodOptions.length)];
                    Platform.runLater(() -> moveToWaiting(id, food)); // Move to waiting stage
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void moveToWaiting(int id, String food) {
        String currentText = orderingContent.getText();
        orderingContent.setText(currentText.replaceFirst("Customer " + id + " is ordering\\n", ""));

        activeOrders--;
        orderingLabel.setText("Ordering (" + activeOrders + "):");

        String orderDetails = "Customer " + id + " is waiting for " + food;
        waitingList.add(orderDetails);
        updateWaitingLabel();

        processQueue();

        moveToKitchen(id, food);
    }

    private void moveToKitchen(int id, String food) {
        String orderDetails = "Customer " + id + " - " + food + " is preparing";
        kitchenList.add(orderDetails);
        updateKitchenLabel();

        // Simulate kitchen preparation
        new Thread(() -> {
            try {
                Thread.sleep(10000); // Simulate kitchen preparation time
                Platform.runLater(() -> moveToServed(id, food)); // Move to served stage
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void moveToServed(int id, String food) {
        String orderDetails = "Customer " + id + " - " + food + " served";
        servedList.add(orderDetails);

        // Remove from waiting and kitchen lists as the order is now served
        waitingList.removeIf(order -> order.startsWith("Customer " + id));
        kitchenList.removeIf(order -> order.startsWith("Customer " + id));

        updateWaitingLabel();
        updateKitchenLabel();
        updateServedLabel();

        // Simulate customer leaving after served
        new Thread(() -> {
            try {
                int leaveTime = random.nextInt(5001) + 10000;
                Thread.sleep(leaveTime); // Random time before the customer leaves
                Platform.runLater(() -> moveToLeft(id)); // Move to left stage
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private synchronized void moveToLeft(int id) {
        long startTime = customerArrivalTimes.get(id);
        long totalTime = System.currentTimeMillis() - startTime;
        leftList.add("Customer " + id + " left after " + formatTime(totalTime));
        customerArrivalTimes.remove(id);

        // Remove from served list as the customer has left
        servedList.removeIf(order -> order.startsWith("Customer " + id));
        updateLeftLabel();
        updateServedLabel();
    }

    private void updateQueueLabel() {
        queueLabel.setText("Queue (" + queue.size() + "):");
        queueContent.setText(String.join("\n", queue.stream().map(i -> "Customer " + i + " is in the queue").toArray(String[]::new)));
    }

    private void updateWaitingLabel() {
        waitingLabel.setText("Waiting (" + waitingList.size() + "):");
        waitingContent.setText(String.join("\n", waitingList));
    }

    private void updateKitchenLabel() {
        kitchenLabel.setText("Kitchen (" + kitchenList.size() + "):");
        kitchenContent.setText(String.join("\n", kitchenList));
    }

    private void updateServedLabel() {
        servedLabel.setText("Served (" + servedList.size() + "):");
        servedContent.setText(String.join("\n", servedList));
    }

    private void updateLeftLabel() {
        leftLabel.setText("Left (" + leftList.size() + "):");
        leftContent.setText(String.join("\n", leftList));
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " sec";
        } else {
            return (seconds / 60) + " min " + (seconds % 60) + " sec";
        }
    }

    private void updateOrderingMachines(int machines) {
        availableMachines = machines;
        processQueue();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
