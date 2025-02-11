package com.restaurant;

import com.example.restaurantsimulator.services.Customer;
import com.example.restaurantsimulator.services.CustomerQueue;
import com.example.restaurantsimulator.services.Menu;
import com.example.restaurantsimulator.services.OrderQueue;

import java.util.Random;
import java.util.Scanner;

public class RestaurantSimulation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        // 🎯 Step 1: User Inputs Simulation Modifiers
        System.out.print("Enter number of chefs: ");
        int numChefs = scanner.nextInt();

        System.out.print("Enter normal customer arrival rate (milliseconds): ");
        int normalCustomerRate = scanner.nextInt();

        System.out.print("Enter rush hour customer arrival rate (milliseconds): ");
        int rushHourCustomerRate = scanner.nextInt();

        System.out.print("Enter how often rush hour occurs (milliseconds): ");
        int rushHourInterval = scanner.nextInt();

        // 🎯 Step 2: Initialize Restaurant Components
        CustomerQueue customerQueue = new CustomerQueue();
        OrderQueue orderQueue = new OrderQueue(numChefs);

        // 🎯 Step 3: Simulate Customer Arrivals with Rush Hour
        new Thread(() -> {
            boolean isRushHour = false;
            long lastRushHourStart = System.currentTimeMillis();

            while (true) {
                Menu.MealType randomMeal = Menu.MealType.values()[random.nextInt(Menu.MealType.values().length)];
                Customer newCustomer = new Customer(randomMeal);
                customerQueue.addCustomer(newCustomer);
                customerQueue.processNextCustomer(orderQueue);

                // Toggle rush hour based on time interval
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRushHourStart > rushHourInterval) {
                    isRushHour = !isRushHour; // Toggle rush hour mode
                    lastRushHourStart = currentTime;
                    System.out.println("🚦 Rush Hour " + (isRushHour ? "STARTED" : "ENDED") + " 🚦");
                }

                int waitTime = isRushHour ? rushHourCustomerRate : normalCustomerRate;

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        // 🎯 Step 4: Simulate Continuous Order Processing
        new Thread(orderQueue::processOrders).start();
    }
}
