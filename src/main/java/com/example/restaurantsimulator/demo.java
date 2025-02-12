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

import com.example.restaurantsimulator.services.Customer;
import com.example.restaurantsimulator.services.Menu;
import com.example.restaurantsimulator.services.OrderQueue;

public class demo extends Application {
    private static final int MAX_MACHINES = 3;
    private Queue<Integer> queue = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
    private Queue<String> kitchenList = new LinkedList<>();
    private Queue<String> servedList = new LinkedList<>();
    private Queue<String> leftList = new LinkedList<>();
    private Queue<String> hiddenOrderList = new LinkedList<>();

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

    private OrderQueue orderQueue; // Using OrderQueue to manage orders

    private int activeKitchenOrders = 0;
    private int availableChefs = 1; // Default chef count

    @Override
    public void start(Stage primaryStage) {
        VBox queueBox = new VBox(10, queueLabel, queueContent);
        VBox orderingBox = new VBox(10, orderingLabel, orderingContent);
        VBox waitingBox = new VBox(10, waitingLabel, waitingContent);
        VBox kitchenBox = new VBox(10, kitchenLabel, kitchenContent);
        VBox servedBox = new VBox(10, servedLabel, servedContent);
        VBox leftBox = new VBox(10, leftLabel, leftContent);

        HBox mainLayout = new HBox(50, queueBox, orderingBox, waitingBox, kitchenBox, servedBox, leftBox);

        // Dropdown for selecting the number of ordering machines
        machineSelector.getItems().addAll(1, 2, 3);
        machineSelector.setValue(1);
        machineSelector.setOnAction(e -> updateOrderingMachines(machineSelector.getValue()));
        ComboBox<Integer> chefSelector = new ComboBox<>();
        chefSelector.getItems().addAll(1, 2, 3, 4, 5); // Adjust max chefs as needed
        chefSelector.setValue(1);
        chefSelector.setOnAction(e -> updateChefs(chefSelector.getValue()));

        VBox controlBox = new VBox(10,
                new Label("🔧 Select Ordering Machines:"), machineSelector,
                new Label("👨‍🍳 Select Number of Chefs:"), chefSelector
        );        VBox root = new VBox(20, mainLayout, controlBox);

        Scene scene = new Scene(root, 1150, 350);


        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        orderQueue = new OrderQueue(availableMachines);  // Initialize the order queue with chefs
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

        startKitchenWorker(); // Start monitoring kitchen availability
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
                    Thread.sleep(7000); // Simulate time for customer to place order

                    // Randomly select a meal from the Menu class
                    int index = random.nextInt(Menu.MealType.values().length);
                    Menu.MealType meal = Menu.MealType.values()[index];

                    // Move the customer to the waiting list and then to the kitchen
                    Platform.runLater(() -> moveToWaiting(id, meal));

                    // Add the order to the OrderQueue for processing
                    orderQueue.addOrder(new Customer(meal), meal);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void moveToKitchen(int id, Menu.MealType meal) {
        if (activeKitchenOrders >= availableChefs) {
            return; // Kitchen is full, don't move customer yet
        }

        activeKitchenOrders++; // A chef starts working

        kitchenList.add("Customer " + id + " - " + meal.name() + " is preparing");
        updateKitchenLabel();

        new Thread(() -> {
            try {
                Thread.sleep(meal.getPrepTime() * 1000L); // Simulate preparation time
                Platform.runLater(() -> moveToServed(id, meal));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void moveToServed(int id, Menu.MealType meal) {
        kitchenList.removeIf(order -> order.startsWith("Customer " + id));
        updateKitchenLabel();

        servedList.add("Customer " + id + " - " + meal.name() + " served");
        updateServedLabel();

        activeKitchenOrders--; // Free up a chef slot for the next order
        processWaitingList();  // Check if a new order can move into the kitchen

        waitingList.removeIf(order -> order.startsWith("Customer " + id));
        updateWaitingLabel();

        new Thread(() -> {
            try {
                int leaveTime = random.nextInt(5001) + 10000;
                Thread.sleep(leaveTime);
                Platform.runLater(() -> moveToLeft(id, meal));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processWaitingList() {
        if (!hiddenOrderList.isEmpty() && activeKitchenOrders < availableChefs) {
            String order = hiddenOrderList.poll();
            if (order != null) {
                String[] parts = order.split(" ");
                int id = Integer.parseInt(parts[0]); // Extract customer ID
                Menu.MealType meal = Menu.MealType.valueOf(parts[1]); // Extract meal type
                moveToKitchen(id, meal);
            }
        }
    }


    private void moveToLeft(int id, Menu.MealType meal) {
        // Get the time when the customer first arrived
        long startTime = customerArrivalTimes.get(id);
        long totalTime = System.currentTimeMillis() - startTime; // Time spent in the restaurant

        // Format the total time
        String timeSpent = formatTime(totalTime);

        // Add the customer to the left list with their total time spent
        leftList.add("Customer " + id + " left after " + timeSpent + " (Ordered " + meal.name() + ")");
        updateLeftLabel();

        // Remove the customer from the served list once they leave
        servedList.removeIf(order -> order.startsWith("Customer " + id));
        updateServedLabel();

        // Remove the customer from the list of arrival times as they have left
        customerArrivalTimes.remove(id);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " sec";
        } else {
            return (seconds / 60) + " min " + (seconds % 60) + " sec";
        }
    }


    private void moveToWaiting(int id, Menu.MealType meal) {
        orderingContent.setText(orderingContent.getText().replace("Customer " + id + " is ordering\n", ""));
        activeOrders--;
        orderingLabel.setText("Ordering (" + activeOrders + "):");

        // Add to visible waiting list for UI
        waitingList.add("Customer " + id + " is waiting for " + meal.name());
        updateWaitingLabel();

        // Add to hidden order list for kitchen processing
        hiddenOrderList.add(id + " " + meal.name());

        processWaitingList(); // Check if we can move an order into the kitchen
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

    private void updateOrderingMachines(int machines) {
        availableMachines = machines;
        orderQueue = new OrderQueue(availableMachines);  // Reinitialize the order queue with new number of chefs
        processQueue();
    }

    // Modifying amount of chefs
    private void updateChefs(int chefs) {
        availableChefs = chefs;
        orderQueue.setChefs(chefs);
        processWaitingList(); // Immediately check if new chefs can start work
    }


    // Kitchen worker
    // Kitchen worker - Monitors and moves waiting customers when space is available
    private void startKitchenWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Check every second

                    Platform.runLater(() -> {
                        if (!hiddenOrderList.isEmpty() && activeKitchenOrders < availableChefs) {
                            String order = hiddenOrderList.poll();
                            if (order != null) {
                                String[] parts = order.split(" ");
                                int id = Integer.parseInt(parts[0]); // Extract customer ID
                                Menu.MealType meal = Menu.MealType.valueOf(parts[1]); // Extract meal type
                                moveToKitchen(id, meal);
                            }
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }




    public static void main(String[] args) {
        launch(args);
    }
}