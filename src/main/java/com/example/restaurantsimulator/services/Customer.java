package com.example.restaurantsimulator.services;

public class Customer {
    private static int idCounter = 1;
    private int id;
    private Menu.MealType order;
    private double satisfaction;
    private boolean orderReceived;
    private boolean isServed; // New boolean to track if the customer is served

    public Customer(Menu.MealType order) {
        this.id = idCounter++;
        this.order = order;
        this.satisfaction = 100.0;
        this.orderReceived = false;
        this.isServed = false;  // Initially, the customer is not served
        startSatisfactionTimer();
    }

    public int getId() {
        return id;
    }

    public Menu.MealType getOrder() {
        return order;
    }

    public double getSatisfaction() {
        return satisfaction;
    }

    public boolean isServed() {  // Getter for isServed
        return isServed;
    }

    public void serve() {  // Method to mark the customer as served
        this.isServed = true;
    }

    // Starts a timer that reduces satisfaction by 0.5% per second
    private void startSatisfactionTimer() {
        new Thread(() -> {
            while (!orderReceived && satisfaction > 0) {
                try {
                    Thread.sleep(1000); // 1 second delay
                    satisfaction -= 0.5;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Stops the timer and logs final satisfaction when the order is received
    public void receiveOrder() {
        orderReceived = true;
        System.out.printf("Customer %d received their %s. Satisfaction: %.2f%%%n", id, order.name(), satisfaction);
    }
}
