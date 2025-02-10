package restaurant;

import restaurant.services.Customer;
import restaurant.services.CustomerQueue;
import restaurant.services.Menu;
import restaurant.services.OrderQueue;

import java.util.Random;

public class RestaurantSimulation {
    public static void main(String[] args) {
        CustomerQueue customerQueue = new CustomerQueue();
        OrderQueue orderQueue = new OrderQueue();
        Random random = new Random();

        // Simulate customer arrivals in a loop
        new Thread(() -> {
            while (true) {
                Menu.MealType randomMeal = Menu.MealType.values()[random.nextInt(Menu.MealType.values().length)];
                Customer newCustomer = new Customer(randomMeal);
                customerQueue.addCustomer(newCustomer);
                customerQueue.processNextCustomer(orderQueue);

                try {
                    Thread.sleep(random.nextInt(5000) + 2000); // New customer every 2-7 seconds
                    // Rush hour fucntion perhaps, and maybe optimize customers incoming
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        // Simulate continuous order processing
        new Thread(orderQueue::processOrders).start();
    }
}