package Model;

public class Menu {
    public enum MealType {
        NORMAL_MEAL("Burger, Fries and a Drink", 5),
        FAMILY_MEAL("Three Burgers, Fries and Drinks", 15),
        MEGA_MEAL("Five Burgers, Fries and Drinks, with two Desserts", 30),
        SUGARY_MEAL("Burger, Fries, Drink and a Dessert", 15);

        private final String description;
        private final int prepTime; // in minutes

        MealType(String description, int prepTime) {
            this.description = description;
            this.prepTime = prepTime;
        }

        public String getDescription() {
            return description;
        }

        public int getPrepTime() {
            return prepTime;
        }
    }
}

