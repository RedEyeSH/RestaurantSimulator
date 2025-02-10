package restaurant.services;

import java.util.*;

public class OrderQueue {
    private PriorityQueue<Menu.MealType> orderQueue;
    private Map<Menu.MealType, Queue<Customer>> orderCustomerMap; // Store multiple customers per meal

    public OrderQueue() {
        this.orderQueue = new PriorityQueue<>(Comparator.comparingInt(Menu.MealType::getPrepTime));
        this.orderCustomerMap = new HashMap<>();
    }

    public void addOrder(Customer customer, Menu.MealType meal) {
        orderQueue.offer(meal);
        orderCustomerMap.putIfAbsent(meal, new LinkedList<>()); // Initialize queue if not exists
        orderCustomerMap.get(meal).offer(customer); // Add customer to meal queue
        System.out.println("Customer " + customer.getId() + " ordered " + meal.name() + ".");
    }

    public void processOrders() {
        while (true) {
            if (!orderQueue.isEmpty()) {
                Menu.MealType meal = orderQueue.poll();
                Queue<Customer> customerQueue = orderCustomerMap.get(meal);
                Customer customer = (customerQueue != null) ? customerQueue.poll() : null;

                new Thread(() -> {
                    System.out.println("Processing order: " + meal.name() + " (Takes " + meal.getPrepTime() + " minutes to prepare)");
                    try {
                        Thread.sleep(meal.getPrepTime() * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(meal.name() + " is ready!");

                    if (customer != null) {
                        customer.receiveOrder();
                    }

                    // Remove meal type if no more customers waiting for it
                    if (customerQueue != null && customerQueue.isEmpty()) {
                        orderCustomerMap.remove(meal);
                    }
                }).start();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}