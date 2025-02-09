package restaurant.services;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class OrderQueue {

    private PriorityQueue<Menu.MealType> orderQueue;
    private Map<Menu.MealType, Customer> orderCustomerMap; // Maps orders to customers

    public OrderQueue() {
        this.orderQueue = new PriorityQueue<>(Comparator.comparingInt(Menu.MealType::getPrepTime));
        this.orderCustomerMap = new HashMap<>();
    }

    // Method to add an order to the queue, now accepting a Customer as well
    public void addOrder(Menu.MealType meal) {
        orderQueue.offer(meal);
        orderCustomerMap.put(meal, customer);  // Now associating the customer with their order
        System.out.println("Customer " + customer.getId() + " ordered " + meal.name() + ".");
    }

    // Method to process orders concurrently
    public void processOrders() {
        while (!orderQueue.isEmpty()) {
            Menu.MealType meal = orderQueue.poll();
            Customer customer = orderCustomerMap.get(meal);
            new Thread(() -> {
                System.out.println("Processing order: " + meal.name() + " (Takes " + meal.getPrepTime() + " minutes to prepare)");
                try {
                    Thread.sleep(meal.getPrepTime() * 1000L); // Simulating meal preparation time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println(meal.name() + " is ready!");
                if (customer != null) {
                    customer.receiveOrder();
                }
                orderCustomerMap.remove(meal);
            }).start();
        }
    }
}
