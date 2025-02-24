package com.example.restaurantsimulator.model;


public class CustomerTimers {
    private long queueStartTime;
    private long queueEndTime;
    private long orderingStartTime;
    private long orderingEndTime;
    private long waitingStartTime;
    private long waitingEndTime;
    private long kitchenStartTime;
    private long kitchenEndTime;
    private long servedStartTime;
    private long servedEndTime;

    public void startQueue() {
        queueStartTime = System.currentTimeMillis();
    }

    public void endQueue() {
        queueEndTime = System.currentTimeMillis();
    }

    public void startOrdering() {
        orderingStartTime = System.currentTimeMillis();
    }

    public void endOrdering() {
        orderingEndTime = System.currentTimeMillis();
    }

    public void startWaiting() {
        waitingStartTime = System.currentTimeMillis();
    }

    public void endWaiting() {
        waitingEndTime = System.currentTimeMillis();
    }

    public void startKitchen() {
        kitchenStartTime = System.currentTimeMillis();
    }

    public void endKitchen() {
        kitchenEndTime = System.currentTimeMillis();
    }

    public void startServed() {
        servedStartTime = System.currentTimeMillis();
    }

    public void endServed() {
        servedEndTime = System.currentTimeMillis();
    }

    public void endTimer() {
        // Print time spent in each stage
        long queueTime = queueEndTime - queueStartTime;
        long orderingTime = orderingEndTime - orderingStartTime;
        long waitingTime = waitingEndTime - waitingStartTime;
        long kitchenTime = kitchenEndTime - kitchenStartTime;
        long servedTime = servedEndTime - servedStartTime;

        System.out.println("Customer's time breakdown:");
        System.out.println("Queue: " + formatTime(queueTime));
        System.out.println("Ordering: " + formatTime(orderingTime));
        System.out.println("Waiting: " + formatTime(waitingTime));
        System.out.println("Kitchen: " + formatTime(kitchenTime));
        System.out.println("Served: " + formatTime(servedTime));
        System.out.println("Total Time in Restaurant: " + formatTime(queueTime + orderingTime + waitingTime + servedTime));
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " sec";
        } else {
            return (seconds / 60) + " min " + (seconds % 60) + " sec";
        }
    }
}


