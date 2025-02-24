package com.example.restaurantsimulator.controller;



import com.example.restaurantsimulator.demo;
import javafx.application.Platform;
import com.example.restaurantsimulator.model.Customer;
import com.example.restaurantsimulator.model.OrderQueue;
import com.example.restaurantsimulator.model.Menu;
import com.example.restaurantsimulator.model.CustomerTimers;
import com.example.restaurantsimulator.view.RestaurantView;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantController {

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

    private int availableChefs = 1;
    private int activeKitchenOrders = 0;

    private RestaurantView view;
    private OrderQueue orderQueue;

    private Map<Integer, CustomerTimers> customerTimers = new ConcurrentHashMap<>();

    public RestaurantController(RestaurantView view) {
        this.view = view;
        this.orderQueue = new OrderQueue(availableMachines);
    }


    public void startSimulation() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);  // Simulate time between customer arrivals
                    synchronized (this) {
                        int id = customerID.getAndIncrement();
                        long startTime = System.currentTimeMillis();
                        Platform.runLater(() -> addCustomerToQueue(id, startTime));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        startKitchenWorker();
    }




    private void addCustomerToQueue(int id, long startTime) {
        customerArrivalTimes.put(id, startTime);
        queue.add(id);

        customerTimers.put(id, new CustomerTimers());
        customerTimers.get(id).startQueue();

        customerList.put(id, new Customer());

        updateQueueLabel();
        processQueue();
        startQueueTimer();

        System.out.println(availableMachines);
    }
    public void updateAvailableMachines(int selectedMachines) {
        this.availableMachines = selectedMachines;
        System.out.println("Selected machines: " + selectedMachines);
        updateOrderingMachines(selectedMachines);
    }

    // Method to update available chefs when the selection changes
    public void updateAvailableChefs(int selectedChefs) {
        this.availableChefs = selectedChefs;
        // Additional logic to adjust the simulation based on available chefs if necessary
        System.out.println("Selected chefs: " + selectedChefs);
        updateChefs(selectedChefs);
    }

    private void processQueue() {
        while (activeOrders < availableMachines && !queue.isEmpty()) {
            int id = queue.poll();
            activeOrders++;
            view.getOrderingLabel().setText("Ordering (" + activeOrders + "):");
            view.getOrderingContent().setText(view.getOrderingContent().getText() + "Customer " + id + " is ordering\n");
            updateQueueLabel();

            customerTimers.get(id).endQueue();
            customerTimers.get(id).startOrdering();

            new Thread(() -> {
                try {
                    int orderingTime = 10000 + new Random().nextInt(11000);
                    Thread.sleep(orderingTime);

                    Menu.MealType[] meals = Menu.MealType.values();
                    if (meals.length == 0) {
                        System.err.println("Error: No meals available for Customer " + id);
                        return;
                    }

                    int index = new Random().nextInt(meals.length);
                    Menu.MealType meal = meals[index];

                    Customer customer = customerList.get(id);
                    if (customer == null) {
                        System.err.println("Error: Customer " + id + " not found.");
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
        view.getOrderingContent().setText(view.getOrderingContent().getText().replace("Customer " + id + " is ordering\n", ""));
        activeOrders--;
        view.getOrderingLabel().setText("Ordering (" + activeOrders + "):");

        if (!waitingList.contains("Customer " + id + " is waiting for " + meal.name())) {
            waitingList.add("Customer " + id + " is waiting for " + meal.name());
        }
        updateWaitingLabel();

        customerTimers.get(id).startWaiting();

        processWaitingList();
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
                int leaveTime = new Random().nextInt(5001) + 10000;
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

    private void updateQueueLabel() {
        view.getQueueLabel().setText("Queue (" + queue.size() + "):");
        StringBuilder sb = new StringBuilder();
        long currentTime = System.currentTimeMillis();

        for (int id : queue) {
            long elapsedTime = currentTime - customerArrivalTimes.get(id);
            sb.append("Customer ").append(id).append(" - Waiting for ")
                    .append(formatTime(elapsedTime)).append("\n");
        }
        view.getQueueContent().setText(sb.toString());
    }

    private void updateWaitingLabel() {
        view.getWaitingLabel().setText("Waiting (" + waitingList.size() + "):");
        view.getWaitingContent().setText(String.join("\n", waitingList));
    }

    private void updateKitchenLabel() {
        view.getKitchenLabel().setText("Kitchen (" + kitchenList.size() + "):");
        view.getKitchenContent().setText(String.join("\n", kitchenList));
    }

    private void updateServedLabel() {
        view.getServedLabel().setText("Served (" + servedList.size() + "):");
        view.getServedContent().setText(String.join("\n", servedList));
    }

    private void updateLeftLabel() {
        view.getLeftLabel().setText("Left (" + leftList.size() + "):");
        view.getLeftContent().setText(String.join("\n", leftList));
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
    private void updateChefs(int chefs) {
        availableChefs = chefs;
        orderQueue.setChefs(chefs);
        processWaitingList(); // Immediately check if new chefs can start work
    }

    private void updateOrderingMachines(int machines) {
        availableMachines = machines;
        orderQueue = new OrderQueue(availableMachines);  // Reinitialize the order queue with new number of chefs
        processQueue();
        System.out.println("Updated ordering machines to " + machines);
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

    private void startKitchenWorker() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);  // Delay to simulate kitchen preparation
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

