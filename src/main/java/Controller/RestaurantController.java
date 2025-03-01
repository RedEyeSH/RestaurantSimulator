package Controller;

import Model.Customer;
import Model.CustomerTimers;
import Model.Menu;
import Model.OrderQueue;
import View.RestaurantView;
import javafx.application.Platform;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantController {

    private static final int MAX_MACHINES = 3;
    private static final int FULL_RESTAURANT = 50; // Defining maximum amount of customers in the restaurant
    private Queue<Integer> queue = new LinkedList<>();
    private Map<Integer, Customer> customerList = new ConcurrentHashMap<>();
    private Queue<String> kitchenList = new LinkedList<>();
    private Queue<String> waitingList = new LinkedList<>();
    private Queue<String> servedList = new LinkedList<>();
    private Queue<String> payList = new LinkedList<>();  // The pay list
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
                    Thread.sleep(2000);  // Simulate time between customer arrivals
                    synchronized (this) {
                        Customer customer = new Customer(); // Create a new customer
                        long startTime = System.currentTimeMillis();
                        Platform.runLater(() -> addCustomerToQueue(customer, startTime));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        startKitchenWorker();
    }


    private int getTotalCustomers() {
        return  activeOrders + waitingList.size() + servedList.size() + payList.size();
        // Orders,     Waiting,             Served
    }



    private void addCustomerToQueue(Customer customer, long startTime) {
        int id = customer.getId(); // Retrieve the already assigned ID
        customerArrivalTimes.put(id, startTime);
        queue.add(id);
        customerTimers.put(id, new CustomerTimers(id));
        customerTimers.get(id).startQueue();
        customerList.put(id, customer);

        updateQueueLabel();
        processQueue();
        startQueueTimer();
    }

    public void updateAvailableMachines(int selectedMachines) {
        this.availableMachines = selectedMachines;
        updateOrderingMachines(selectedMachines);
    }

    // Method to update available chefs when the selection changes
    public void updateAvailableChefs(int selectedChefs) {
        this.availableChefs = selectedChefs;
        updateChefs(selectedChefs);
    }

    private void processQueue() {
        System.out.println("Total amount of customers in the restaurant: " + getTotalCustomers());
        while (activeOrders < availableMachines && !queue.isEmpty()) {
            if (getTotalCustomers() >= FULL_RESTAURANT) {
                return; // Stop processing if the restaurant is full
            }

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
                int leaveTime =  meal.getPrepTime() * 1000 + 10000; // Simulate eating time + 5 seconds
                Thread.sleep(leaveTime);
                Platform.runLater(() -> moveToPayList(id, meal));  // Move to pay list
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void moveToPayList(int id, Menu.MealType meal) {
        // Remove the customer from the served list and add them to the payList
        servedList.removeIf(order -> order.startsWith("Customer " + id));
        updateServedLabel();

        payList.add("Customer " + id + " - " + meal.name() + " is paying");
        updatePayListLabel();

        customerTimers.get(id).startPay();
        customerTimers.get(id).endServed();

        // Wait for 5 seconds before moving the customer to the left list
        new Thread(() -> {
            try {
                Thread.sleep(10000);
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

        // Add customer to left list after the payment
        leftList.add("Customer " + id + " left after " + timeSpent + " (Ordered " + meal.name() + ")");
        updateLeftLabel();

        // Stop the timer when the customer leaves
        customerTimers.get(id).endPay();
        customerTimers.get(id).endTimer();

        // Remove the customer from the payList as they have left
        payList.removeIf(order -> order.startsWith("Customer " + id));
        updatePayListLabel();

        // Remove the customer from the arrival times
        customerArrivalTimes.remove(id);
    }


    private void updateQueueLabel() {
        int roundedSize = getRoundedSize(queue.size());

        // Set the Queue label with both the current queue size and the rounded size
        view.getQueueLabel().setText("Queue (" + queue.size() + "): Avg: " + roundedSize);

        // Update the content with the queue details
        StringBuilder sb = new StringBuilder();
        long currentTime = System.currentTimeMillis();

        for (int id : queue) {
            long elapsedTime = currentTime - customerArrivalTimes.get(id);
            sb.append("Customer ").append(id).append(" - Waiting for ")
                    .append(formatTime(elapsedTime)).append("\n");
        }
        view.getQueueContent().setText(sb.toString()); // Display the queue details
    }



    private void updateWaitingLabel() {
        int roundedSize = getRoundedSize(waitingList.size());

        view.getWaitingLabel().setText("Waiting (" + waitingList.size() + ") Avg: " + roundedSize);
        view.getWaitingContent().setText(String.join("\n", waitingList));

    }

    private void updateKitchenLabel() {
        view.getKitchenLabel().setText("Kitchen (" + kitchenList.size() + "):");
        view.getKitchenContent().setText(String.join("\n", kitchenList));
    }

    private void updateServedLabel() {
        int roundedSize = getRoundedSize(servedList.size());

        view.getServedLabel().setText("Served (" + servedList.size() + ") Avg: " + roundedSize);
        view.getServedContent().setText(String.join("\n", servedList));
    }

    private void updatePayListLabel() {
        view.getPayLabel().setText("Paying (" + payList.size() + "):");
        view.getPayContent().setText(String.join("\n", payList));
    }

    private void updateLeftLabel() {
        view.getLeftLabel().setText("Left (" + leftList.size() + "):");
        view.getLeftContent().setText(String.join("\n", leftList));
    }

    private int getRoundedSize(int size) {
        int remainder = size % 5;
        if (remainder == 0) {
            return size; // Already a multiple of 5
        }
        return (remainder >= 3) ? ((size / 5 + 1) * 5) : ((size / 5) * 5);
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


