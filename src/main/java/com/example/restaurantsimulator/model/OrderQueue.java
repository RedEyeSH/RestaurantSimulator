package com.example.restaurantsimulator.model;


import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

public class OrderQueue {
    private PriorityBlockingQueue<Customer> orderQueue;
    private int chefCount;

    public OrderQueue(int chefCount) {
        this.chefCount = chefCount;
        orderQueue = new PriorityBlockingQueue<>(10, Comparator.comparingInt(c -> c.getId()));
    }

    public void addOrder(Customer customer, Menu.MealType meal) {
        orderQueue.add(customer);
    }

    public void setChefs(int chefs) {
        chefCount = chefs;
    }
}

