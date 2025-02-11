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
                    Thread.sleep(5000);
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

    private void processQueue() {
        while (activeOrders < availableMachines && !queue.isEmpty()) {
            int id = queue.poll();
            activeOrders++;
            orderingLabel.setText("Ordering (" + activeOrders + "):");
            orderingContent.setText(orderingContent.getText() + "Customer " + id + " is ordering\n");
            updateQueueLabel();

            new Thread(() -> {
                try {
                    Thread.sleep(7000);
                    String food = foodOptions[random.nextInt(foodOptions.length)];
                    Platform.runLater(() -> moveToWaiting(id, food));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void moveToWaiting(int id, String food) {
        orderingContent.setText(orderingContent.getText().replace("Customer " + id + " is ordering\n", ""));
        activeOrders--;
        orderingLabel.setText("Ordering (" + activeOrders + "):");

        waitingList.add("Customer " + id + " is waiting");
        updateWaitingLabel();
        processQueue();
        moveToKitchen(id, food);
    }

    private void moveToKitchen(int id, String food) {
        waitingList.removeIf(order -> order.startsWith("Customer " + id));
        updateWaitingLabel();

        kitchenList.add("Customer " + id + " - " + food + " is preparing");
        updateKitchenLabel();

        new Thread(() -> {
            try {
                Thread.sleep(10000);
                Platform.runLater(() -> moveToServed(id, food));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void moveToServed(int id, String food) {
        kitchenList.removeIf(order -> order.startsWith("Customer " + id));
        updateKitchenLabel();

        servedList.add("Customer " + id + " - " + food + " served");
        updateServedLabel();

        new Thread(() -> {
            try {
                int leaveTime = random.nextInt(5001) + 10000;
                Thread.sleep(leaveTime);
                Platform.runLater(() -> moveToLeft(id));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void moveToLeft(int id) {
        long startTime = customerArrivalTimes.get(id);
        long totalTime = System.currentTimeMillis() - startTime;
        leftList.add("Customer " + id + " left after " + formatTime(totalTime));
        customerArrivalTimes.remove(id);

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
