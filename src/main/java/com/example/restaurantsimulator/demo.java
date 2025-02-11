package com.example.restaurantsimulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class demo extends Application {
    private Queue<Integer> queue = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
    private Queue<String> kitchenList = new LinkedList<>();
    private Queue<String> servedList = new LinkedList<>();
    private Queue<String> leftList = new LinkedList<>();

    private Map<Integer, Long> customerArrivalTimes = new ConcurrentHashMap<>(); // Store each customer's arrival time

    private AtomicInteger customerID = new AtomicInteger(1);

    private Label queueLabel = new Label("Queue (0):");
    private Label orderingLabel = new Label("Ordering:");
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

    {
        String headingStyle = "-fx-font-size: 18px; -fx-font-weight: bold;";

        queueLabel.setStyle(headingStyle);
        orderingLabel.setStyle(headingStyle);
        waitingLabel.setStyle(headingStyle);
        kitchenLabel.setStyle(headingStyle);
        servedLabel.setStyle(headingStyle);
        leftLabel.setStyle(headingStyle);
    }

    private boolean machineFree = true;
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

        HBox root = new HBox(70, queueBox, orderingBox, waitingBox, kitchenBox, servedBox, leftBox);
        Scene scene = new Scene(root, 1050, 250);

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
        customerArrivalTimes.put(id, startTime); // Store when customer arrives
        queue.add(id);
        updateQueueLabel();
        processQueue();
    }

    private void processQueue() {
        if (machineFree && !queue.isEmpty()) {
            int id = queue.poll();
            machineFree = false;
            orderingContent.setText("Customer " + id + " is ordering");
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
        orderingContent.setText("");
        String orderDetails = "Customer " + id + " is waiting";
        waitingList.add(orderDetails);
        updateWaitingLabel();
        machineFree = true;
        processQueue();
        moveToKitchen(id, food);
    }

    private void moveToKitchen(int id, String food) {
        String orderDetails = "Customer " + id + " - " + food + " is preparing";
        kitchenList.add(orderDetails);
        updateWaitingLabel();
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

        String orderDetails = "Customer " + id + " - " + food + " served";
        servedList.add(orderDetails);

        waitingList.removeIf(order -> order.startsWith("Customer " + id));
        kitchenList.removeIf(order -> order.startsWith("Customer " + id));

        updateWaitingLabel();
        updateKitchenLabel();
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
        String leftDetails = "Customer " + id + " left after " + formatTime(totalTime);
        leftList.add(leftDetails);
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

    public static void main(String[] args) {
        launch(args);
    }
}
