package Services;

import com.example.restaurantsimulator.services.Customer;
import com.example.restaurantsimulator.services.OrderQueue;

import java.util.LinkedList;
import java.util.Queue;

public class CustomerQueue {

    private Queue<com.example.restaurantsimulator.services.Customer> customerQueue;

    public CustomerQueue() {
        this.customerQueue = new LinkedList<>();
    }

    // Method to add a customer to the queue
    public void addCustomer(com.example.restaurantsimulator.services.Customer customer) {
        customerQueue.add(customer);
        System.out.println("Customer " + customer.getId() + " added to the queue.");
    }

    // Method to process the next customer
    public void processNextCustomer(OrderQueue orderQueue) {
        if (!customerQueue.isEmpty()) {
            Customer customer = customerQueue.poll();
            System.out.println("Customer " + customer.getId() + " is placing an order: " + customer.getOrder().name());
            orderQueue.addOrder(customer, customer.getOrder());
        } else {
            System.out.println("No customers in the queue.");
        }
    }

    public boolean isEmpty() {
        return customerQueue.isEmpty();
    }
}
