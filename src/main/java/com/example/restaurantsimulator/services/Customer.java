package com.example.restaurantsimulator.services;

public class Customer {
    private static int idCounter = 1;
    private int id;
    private Menu.MealType order;
    private double satisfaction;
    private boolean orderReceived;

    public Customer(Menu.MealType order) {
        this.id = idCounter++;
        this.order = order;
        this.satisfaction = 100.0;
        this.orderReceived = false;
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

/*
public class Customer {
    private int id;
    private static int idCount = 1;

    private double patienceLevel;
    private double eatingTime;

    private ArrayList<Order> orderList;

    public Customer() {
        this.id = idCount++;
        this.patienceLevel = new Random().nextDouble() * 10 + 5;
        this.eatingTime = new Random().nextDouble() * 20 + 10;
        this.orderList = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void addOrder(Order order) {
        orderList.add(order);
    }

    public ArrayList<Order> getOrderList() {
        return orderList;
    }

    public double getPatienceLevel() {
        return patienceLevel;
    }

    private double getEatingTime() {
        return eatingTime;
    }

    public String displayCustomer() {
        return "Customer Id: " + getId();
    }
}

*/