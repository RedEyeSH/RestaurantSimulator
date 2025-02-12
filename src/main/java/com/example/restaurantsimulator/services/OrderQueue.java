package com.example.restaurantsimulator.services;

import java.util.*;
import java.util.concurrent.*;

public class OrderQueue {
    private PriorityBlockingQueue<Menu.MealType> orderQueue; // Thread-safe priority queue
    private Map<Menu.MealType, Queue<Customer>> orderCustomerMap;
    private ExecutorService chefPool; // Thread pool for chefs
    private int availableChefs;

    public OrderQueue(int numChefs) {
        this.orderQueue = new PriorityBlockingQueue<>(10, Comparator.comparingInt(Menu.MealType::getPrepTime));
        this.orderCustomerMap = new ConcurrentHashMap<>();
        this.chefPool = Executors.newFixedThreadPool(numChefs); // Creates a pool of chefs
    }

    public void addOrder(Customer customer, Menu.MealType meal) {
        orderQueue.put(meal); // Thread-safe blocking insertion
        orderCustomerMap.putIfAbsent(meal, new ConcurrentLinkedQueue<>());
        orderCustomerMap.get(meal).offer(customer);
        System.out.println("Customer " + customer.getId() + " ordered " + meal.name() + ".");
    }

    public void setChefs(int chefs) {
        this.availableChefs = chefs;
    }


    public void processOrders() {
        new Thread(() -> {
            while (true) {
                try {
                    Menu.MealType meal = orderQueue.take(); // Waits for an order if empty
                    Queue<Customer> customerQueue = orderCustomerMap.get(meal);
                    Customer customer = (customerQueue != null) ? customerQueue.poll() : null;

                    System.out.println("Chef is processing: " + meal.name()); // Add this line

                    chefPool.submit(() -> {  // Assigns order to an available chef
                        System.out.println("Processing order: " + meal.name() + " (Takes " + meal.getPrepTime() + " minutes)");
                        try {
                            Thread.sleep(meal.getPrepTime() * 1000L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println(meal.name() + " is ready!"); // Add this line

                        if (customer != null) {
                            customer.receiveOrder();
                        }

                        if (customerQueue != null && customerQueue.isEmpty()) {
                            orderCustomerMap.remove(meal);
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}