package com.example.restaurantsimulator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// ---------------------- Customer Class ----------------------
class Customer {
    private int id;
    private String order;
    private int patienceLevel;
    private int remainingPatience;
    private int arrivalTime;

    public Customer(int id, String order, int patienceLevel, int arrivalTime) {
        this.id = id;
        this.order = order;
        this.patienceLevel = patienceLevel;
        this.remainingPatience = patienceLevel;
        this.arrivalTime = arrivalTime;
    }

    public int getId() { return id; }
    public String getOrder() { return order; }
    public int getPatienceLevel() { return patienceLevel; }
    public int getRemainingPatience() { return remainingPatience; }
    public int getArrivalTime() { return arrivalTime; }

    public void decrementPatience() { remainingPatience--; }

    public boolean hasPatienceLeft() {
        return remainingPatience > 0;
    }

    @Override
    public String toString() {
        return "Customer #" + id + " - Order: " + order + " (Remaining Patience: " + remainingPatience + "s)";
    }
}

// ---------------------- Order Class ----------------------
class Order {
    private int orderId;
    private String foodItem;
    private int preparationTime;
    private String status;
    private int createdAtTime;
    private Customer customer;

    public Order(int orderId, String foodItem, int preparationTime, int createdAtTime, Customer customer) {
        this.orderId = orderId;
        this.foodItem = foodItem;
        this.preparationTime = preparationTime;
        this.status = "Waiting";
        this.createdAtTime = createdAtTime;
        this.customer = customer;
    }

    public int getOrderId() { return orderId; }
    public String getFoodItem() { return foodItem; }
    public int getPreparationTime() { return preparationTime; }
    public String getStatus() { return status; }
    public int getCreatedAtTime() { return createdAtTime; }
    public Customer getCustomer() { return customer; }

    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Order #" + orderId + ": " + foodItem + " for " + customer + " - Status: " + status;
    }
}

// ---------------------- Menu Class ----------------------
class Menu {
    public static String[] getAvailableItems() {
        return new String[]{"🍔 Burger - 5s", "🍕 Pizza - 8s", "🥗 Salad - 3s", "🍝 Pasta - 6s"};
    }

    public static int getPreparationTime(String foodItem) {
        return switch (foodItem) {
            case "🍔 Burger - 5s" -> 5;
            case "🍕 Pizza - 8s" -> 8;
            case "🥗 Salad - 3s" -> 3;
            case "🍝 Pasta - 6s" -> 6;
            default -> 5;
        };
    }
}

// ---------------------- Order Queue ----------------------
class OrderQueue {
    private BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();

    public void addOrder(Order order) {
        orderQueue.add(order);
    }

    public Order getNextOrder() throws InterruptedException {
        return orderQueue.poll(1, TimeUnit.SECONDS);
    }

    public boolean isEmpty() {
        return orderQueue.isEmpty();
    }

    public BlockingQueue<Order> getQueue() {
        return orderQueue;
    }
}

// ---------------------- Customer Generator ----------------------
class CustomerGenerator {
    private OrderQueue orderQueue;
    private Random random = new Random();
    private int customerId = 1;

    public CustomerGenerator(OrderQueue orderQueue) {
        this.orderQueue = orderQueue;
    }

    public void startGenerating() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            String foodItem = Menu.getAvailableItems()[random.nextInt(4)];
            int prepTime = Menu.getPreparationTime(foodItem);
            int patience = random.nextInt(20) + 10;
            Customer customer = new Customer(customerId++, foodItem, patience, Main.getCurrentTime());

            Order order = new Order(customer.getId(), foodItem, prepTime, Main.getCurrentTime(), customer);
            orderQueue.addOrder(order);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}

// ---------------------- JavaFX UI Application ----------------------
public class Main extends Application {
    private OrderQueue orderQueue = new OrderQueue();
    private static int simulationTime = 0;
    private Label clockLabel;
    private ListView<String> orderListView;
    private ListView<String> completedOrdersListView;
    private ListView<String> leftOrdersListView;
    private Order currentOrder = null;
    private Timeline preparationTimer;

    @Override
    public void start(Stage primaryStage) {
        showStartScene(primaryStage);
    }

    private void showStartScene(Stage primaryStage) {
        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(20);
        root.setBackground(new Background(new BackgroundFill(Color.LIGHTYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

        Label welcomeLabel = new Label("🍽️ Welcome to Restaurant Simulator");
        welcomeLabel.setFont(new Font("Arial", 24));
        welcomeLabel.setTextFill(Color.DARKBLUE);

        ImageView logo = new ImageView(new Image("file:restaurant.png"));
        logo.setFitHeight(100);
        logo.setFitWidth(100);

        Button startButton = new Button("Start Simulation");
        startButton.setStyle("-fx-background-color: #ffcc00; -fx-font-size: 16px;");
        startButton.setOnAction(event -> showSimulationScene(primaryStage));

        root.getChildren().addAll(logo, welcomeLabel, startButton);
        Scene startScene = new Scene(root, 400, 300);
        primaryStage.setScene(startScene);
        primaryStage.show();
    }

    private void showSimulationScene(Stage primaryStage) {
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);
        root.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

        clockLabel = new Label("⏳ Time: 0s");
        clockLabel.setFont(new Font("Arial", 18));

        Label titleLabel = new Label("🍽️ Restaurant Simulation");
        titleLabel.setFont(new Font("Arial", 22));
        titleLabel.setTextFill(Color.DARKBLUE);

        orderListView = new ListView<>();
        orderListView.setPrefHeight(200);

        completedOrdersListView = new ListView<>();
        completedOrdersListView.setPrefHeight(200);
        completedOrdersListView.setStyle("-fx-background-color: #E0FFE0;");

        leftOrdersListView = new ListView<>();
        leftOrdersListView.setPrefHeight(200);
        leftOrdersListView.setStyle("-fx-background-color: #FFDDDD;");

        root.getChildren().addAll(clockLabel, titleLabel, orderListView, new Label("Completed Orders:"), completedOrdersListView,
                new Label("Customers Who Left:"), leftOrdersListView);
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setScene(scene);

        startSimulationClock();
        new CustomerGenerator(orderQueue).startGenerating();
        processOrders();
    }

    private void startSimulationClock() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            simulationTime++;
            clockLabel.setText("⏳ Time: " + simulationTime + "s");
            updatePatience();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void processOrders() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (!orderQueue.isEmpty()) {
                try {
                    if (currentOrder == null) {
                        currentOrder = orderQueue.getNextOrder();
                        currentOrder.setStatus("⏳ Preparing...");
                        updateOrderList();
                        prepareOrder();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updatePatience() {
        for (Order order : orderQueue.getQueue()) {
            Customer customer = order.getCustomer();
            if (customer.hasPatienceLeft()) {
                customer.decrementPatience();
            } else {
                orderQueue.getQueue().remove(order);
                order.setStatus("❌ Customer Left (No Patience)");
                leftOrdersListView.getItems().add(customer.toString() + " - Order: " + order.getFoodItem());
                updateOrderList();
            }
        }
    }

    private void prepareOrder() {
        if (currentOrder != null) {
            preparationTimer = new Timeline(new KeyFrame(Duration.seconds(currentOrder.getPreparationTime()), event -> {
                currentOrder.setStatus("✅ Ready!");
                completedOrdersListView.getItems().add(currentOrder.toString()); // Add to Completed Orders
                updateOrderList();
                currentOrder = null; // Ready for the next order
            }));
            preparationTimer.setCycleCount(1);
            preparationTimer.play();
        }
    }

    private void updateOrderList() {
        Platform.runLater(() -> {
            orderListView.getItems().clear();
            for (Order order : orderQueue.getQueue()) {
                orderListView.getItems().add(order.toString());
            }

            if (currentOrder != null) {
                orderListView.getItems().add("[⏳ PREPARING] " + currentOrder.toString());
            }
        });
    }

    public static int getCurrentTime() {
        return simulationTime;
    }

    public static void main(String[] args) {
        launch(args);
    }
}


/*

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class demo extends Application {
    private Queue<Integer> queue = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
    private AtomicInteger customerID = new AtomicInteger(1);

    private Label queueLabel = new Label("Queue:");
    private Label orderingLabel = new Label("Ordering:");
    private Label waitingLabel = new Label("Waiting:");

    private Label queueContent = new Label();
    private Label orderingContent = new Label();
    private Label waitingContent = new Label();

    private boolean machineFree = true;
    private Random random = new Random();
    private String[] foodOptions = {"Burger", "Pizza", "Pasta", "Sushi", "Salad"};

    @Override
    public void start(Stage primaryStage) {
        VBox queueBox = new VBox(10, queueLabel, queueContent);
        VBox orderingBox = new VBox(10, orderingLabel, orderingContent);
        VBox waitingBox = new VBox(10, waitingLabel, waitingContent);

        HBox root = new HBox(70, queueBox, orderingBox, waitingBox);
        Scene scene = new Scene(root, 400, 250);

        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        startSimulation();
    }

    private void startSimulation() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // New customer every 5 seconds
                    int id = customerID.getAndIncrement();
                    Platform.runLater(() -> addCustomerToQueue(id));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addCustomerToQueue(int id) {
        queue.add(id);
        updateQueueLabel();
        processQueue();
    }

    private void processQueue() {
        if (machineFree && !queue.isEmpty()) {
            int id = queue.poll();
            machineFree = false;
            orderingContent.setText("Customer " + id);
            updateQueueLabel();

            new Thread(() -> {
                try {
                    Thread.sleep(8000); // Ordering takes 8 seconds
                    String food = foodOptions[random.nextInt(foodOptions.length)];
                    Platform.runLater(() -> moveToWaiting(id, food));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void moveToWaiting(int id, String food) {
        orderingContent.setText(""); // Clear ordering section
        waitingList.add("Customer " + id + " - " + food);
        updateWaitingLabel();
        machineFree = true;
        processQueue();
    }

    private void updateQueueLabel() {
        queueContent.setText(String.join("\n", queue.stream().map(i -> "Customer " + i).toArray(String[]::new)));
    }

    private void updateWaitingLabel() {
        waitingContent.setText(String.join("\n", waitingList));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/