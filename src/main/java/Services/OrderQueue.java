package Services;

import com.example.restaurantsimulator.services.Customer;
import com.example.restaurantsimulator.services.Menu;

import java.util.*;
import java.util.concurrent.*;

public class OrderQueue {
    private PriorityBlockingQueue<com.example.restaurantsimulator.services.Menu.MealType> orderQueue; // Thread-safe priority queue
    private Map<com.example.restaurantsimulator.services.Menu.MealType, Queue<com.example.restaurantsimulator.services.Customer>> orderCustomerMap;
    private ExecutorService chefPool; // Thread pool for chefs
    private int availableChefs;

    public OrderQueue(int numChefs) {
        this.orderQueue = new PriorityBlockingQueue<>(10, Comparator.comparingInt(com.example.restaurantsimulator.services.Menu.MealType::getPrepTime));
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

}