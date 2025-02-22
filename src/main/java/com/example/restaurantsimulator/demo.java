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
    private Map<Integer, Customer> customerList = new ConcurrentHashMap<>();
    private Queue<String> kitchenList = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
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

    private OrderQueue orderQueue;

    private int activeKitchenOrders = 0;
    private int availableChefs = 1; // Default chef count

    // Store timers for each customer at each stage
    private Map<Integer, CustomerTimers> customerTimers = new ConcurrentHashMap<>();

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
        machineSelector.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10); // Adjust max machines as needed
        machineSelector.setValue(1);
        machineSelector.setOnAction(e -> updateOrderingMachines(machineSelector.getValue()));

        ComboBox<Integer> chefSelector = new ComboBox<>();
        chefSelector.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10); // Adjust max chefs as needed
        chefSelector.setValue(1);
        chefSelector.setOnAction(e -> updateChefs(chefSelector.getValue()));

        VBox controlBox = new VBox(10,
                new Label("🔧 Select Ordering Machines:"), machineSelector,
                new Label("👨‍🍳 Select Number of Chefs:"), chefSelector
        );
        VBox root = new VBox(20, mainLayout, controlBox);

        Scene scene = new Scene(root, 1150, 350);

        primaryStage.setTitle("Restaurant Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing application...");
            System.exit(0);
        });

        orderQueue = new OrderQueue(availableMachines);  // Initialize the order queue with chefs
        startSimulation();
    }

    private void startSimulation() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);  // Simulate time between customer arrivals
                    synchronized (this) {
                        int id = customerID.getAndIncrement();  // Increment and get customer ID safely
                        long startTime = System.currentTimeMillis();
                        Platform.runLater(() -> addCustomerToQueue(id, startTime));  // Use id directly here
                    }
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

        customerTimers.put(id, new CustomerTimers());
        customerTimers.get(id).startQueue();

        customerList.put(id, new Customer()); // Create customer without a meal

        updateQueueLabel();
        processQueue();
        startQueueTimer();
    }


    private void startQueueTimer() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::updateQueueLabel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processQueue() {
        while (activeOrders < availableMachines && !queue.isEmpty()) {
            int id = queue.poll();
            activeOrders++;
            orderingLabel.setText("Ordering (" + activeOrders + "):");
            orderingContent.setText(orderingContent.getText() + "Customer " + id + " is ordering\n");
            updateQueueLabel();

            customerTimers.get(id).endQueue();
            customerTimers.get(id).startOrdering();

            new Thread(() -> {
                try {
                    int orderingTime = 10000 + random.nextInt(11000);
                    Thread.sleep(orderingTime);

                    // Ensure meal selection is valid
                    Menu.MealType[] meals = Menu.MealType.values();
                    if (meals.length == 0) {
                        System.err.println("Error: No meals available for Customer " + id);
                        return; // Prevents a crash
                    }

                    int index = random.nextInt(meals.length);
                    Menu.MealType meal = meals[index];

                    Customer customer = customerList.get(id);
                    if (customer == null) {
                        System.err.println("Error: Customer " + id + " not found in customerList.");
                        return;
                    }

                    customer.assignOrder(meal);
                    Platform.runLater(() -> moveToWaiting(id, meal));
                    orderQueue.addOrder(customer, meal);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void moveToWaiting(int id, Menu.MealType meal) {
        customerTimers.get(id).endOrdering();
        orderingContent.setText(orderingContent.getText().replace("Customer " + id + " is ordering\n", ""));
        activeOrders--;
        orderingLabel.setText("Ordering (" + activeOrders + "):");

        // Add to visible waiting list for UI, but avoid duplicates
        if (!waitingList.contains("Customer " + id + " is waiting for " + meal.name())) {
            waitingList.add("Customer " + id + " is waiting for " + meal.name());
        }
        updateWaitingLabel();

        // Start the timer when the customer enters the waiting list
        customerTimers.get(id).startWaiting();

        processWaitingList(); // Check if we can move an order into the kitchen
        processQueue();
    }

    private void moveToKitchen(int id, Menu.MealType meal) {
        if (meal == null) {
            System.err.println("Error: Meal is null for Customer " + id + ". Skipping kitchen process.");
            return; // Prevents crash
        }

        if (activeKitchenOrders >= availableChefs) {
            return; // Kitchen is full, don't move customer yet
        }

        activeKitchenOrders++; // A chef starts working

        kitchenList.add("Customer " + id + " - " + meal.name() + " is preparing");
        updateKitchenLabel();

        // Start timer when the customer enters kitchen
        customerTimers.get(id).startKitchen();

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

        // Stop timer when customer enters served list
        customerTimers.get(id).startServed();
        customerTimers.get(id).endWaiting();
        customerTimers.get(id).endKitchen();

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

    private void moveToLeft(int id, Menu.MealType meal) {
        // Get the time when the customer first arrived
        long startTime = customerArrivalTimes.getOrDefault(id, System.currentTimeMillis()); // Prevent null
        long totalTime = System.currentTimeMillis() - startTime; // Time spent in the restaurant

        // Format the total time
        String timeSpent = formatTime(totalTime);

        // Add the customer to the left list with their total time spent
        leftList.add("Customer " + id + " left after " + timeSpent + " (Ordered " + meal.name() + ")");
        updateLeftLabel();

        // Stop the timer when the customer leaves
        customerTimers.get(id).endServed();
        customerTimers.get(id).endTimer();

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

    private void processWaitingList() {
        if (!customerList.isEmpty()) {
            for (Map.Entry<Integer, Customer> entry : customerList.entrySet()) {
                Customer customer = entry.getValue();
                if (!customer.isServed() && activeKitchenOrders < availableChefs) {
                    Menu.MealType meal = customer.getOrder();

                    if (meal == null) {
                        continue; // Skip this customer
                    }

                    moveToKitchen(entry.getKey(), meal);
                    customer.serve();
                }
            }
        }
    }

    private void updateQueueLabel() {
        queueLabel.setText("Queue (" + queue.size() + "):");
        StringBuilder sb = new StringBuilder();
        long currentTime = System.currentTimeMillis();

        for (int id : queue) {
            long elapsedTime = currentTime - customerArrivalTimes.get(id);
            sb.append("Customer ").append(id).append(" - Waiting for ")
                    .append(formatTime(elapsedTime)).append("\n");
        }
        queueContent.setText(sb.toString());
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

    private void updateChefs(int chefs) {
        availableChefs = chefs;
        orderQueue.setChefs(chefs);
        processWaitingList(); // Immediately check if new chefs can start work
    }

    private void startKitchenWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Check every second

                    Platform.runLater(() -> {
                        if (activeKitchenOrders < availableChefs) {
                            processWaitingList(); // Move customers into kitchen if available
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

    class CustomerTimers {
        private long queueStartTime;
        private long queueEndTime;
        private long orderingStartTime;
        private long orderingEndTime;
        private long waitingStartTime;
        private long waitingEndTime;
        private long kitchenStartTime;
        private long kitchenEndTime;
        private long servedStartTime;
        private long servedEndTime;

        void startQueue() {
            queueStartTime = System.currentTimeMillis();
        }

        void endQueue() {
            queueEndTime = System.currentTimeMillis();
        }

        void startOrdering() {
            orderingStartTime = System.currentTimeMillis();
        }

        void endOrdering() {
            orderingEndTime = System.currentTimeMillis();
        }

        void startWaiting() {
            waitingStartTime = System.currentTimeMillis();
        }

        void endWaiting() {
            waitingEndTime = System.currentTimeMillis();
        }

        void startKitchen() {
            kitchenStartTime = System.currentTimeMillis();
        }

        void endKitchen() {
            kitchenEndTime = System.currentTimeMillis();
        }

        void startServed() {
            servedStartTime = System.currentTimeMillis();
        }

        void endServed() {
            servedEndTime = System.currentTimeMillis();
        }

        void endTimer() {
            // Print time spent in each stage
            long queueTime = queueEndTime - queueStartTime;
            long orderingTime = orderingEndTime - orderingStartTime;
            long waitingTime = waitingEndTime - waitingStartTime;
            long kitchenTime = kitchenEndTime - kitchenStartTime;
            long servedTime = servedEndTime - servedStartTime;

            System.out.println("Customer's time breakdown:");
            System.out.println("Queue: " + formatTime(queueTime));
            System.out.println("Ordering: " + formatTime(orderingTime));
            System.out.println("Waiting: " + formatTime(waitingTime));
            System.out.println("Kitchen: " + formatTime(kitchenTime));
            System.out.println("Served: " + formatTime(servedTime));
            System.out.println("Total Time in Restaurant: " + formatTime(queueTime + orderingTime + waitingTime + servedTime));
        }
    }
}
