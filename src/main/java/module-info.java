module com.example.restaurantsimulator {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.restaurantsimulator to javafx.fxml;
    exports com.example.restaurantsimulator;
    exports com.restaurant;
    opens com.restaurant to javafx.fxml;
}