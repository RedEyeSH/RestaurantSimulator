package com.example.restaurantsimulator.services;

public class Customer {
    private static int idCounter = 1;
    private int id;
    private Menu.MealType order;
    private double satisfaction;
    private boolean orderReceived;
    private boolean isServed;

    public Customer() {
        this.id = idCounter++;
        this.satisfaction = 100.0;
        this.orderReceived = false;
        this.isServed = false;
        startSatisfactionTimer();
    }

    public int getId() {
        return id;
    }

    public Menu.MealType getOrder() {
        return order;
    }

    public void assignOrder(Menu.MealType meal) {
        this.order = meal;
    }

    public double getSatisfaction() {
        return satisfaction;
    }

    public boolean isServed() {
        return isServed;
    }

    public void serve() {
        this.isServed = true;
    }

    private void startSatisfactionTimer() {
        new Thread(() -> {
            while (!orderReceived && satisfaction > 0) {
                try {
                    Thread.sleep(1000);
                    satisfaction -= 0.5;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void receiveOrder() {
        orderReceived = true;
        System.out.printf("Customer %d received their %s. Satisfaction: %.2f%%%n", id, order.name(), satisfaction);
    }
}
