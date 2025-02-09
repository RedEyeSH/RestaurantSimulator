package restaurant.services;
import restaurant.services.Menu;

import restaurant.services.Menu;

import java.util.Random;
import java.util.ArrayList;

public class Customer {
    private static int idCounter = 1;
    private int id;
    private Menu.MealType order;

    public Customer(Menu.MealType order) {
        this.id = idCounter++;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public Menu.MealType getOrder() {
        return order;
    }

    public void receiveOrder() {
        System.out.println("Customer " + id + " has received their " + order.name() + ".");
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