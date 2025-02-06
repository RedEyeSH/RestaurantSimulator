package restaurant.models;

import java.util.Random;

public class Customer {
    private int id;
    private String order;
    private double patienceLevel;
    private double eatingTime;

    public Customer(int id, String order) {
        this.id = id;
        this.order = order;
        this.patienceLevel = new Random().nextDouble() * 10 + 5;
        this.eatingTime = new Random().nextDouble() * 20 + 10;
    }

    public int getId() {
        return id;
    }

    public String getOrder() {
        return order;
    }

    public double getPatienceLevel() {
        return patienceLevel;
    }

    private double getEatingTime() {
        return eatingTime;
    }

    public static void main(String[] args) {

    }
}