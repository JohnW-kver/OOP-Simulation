module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.dyn4j;
    requires javafx.graphics;

    opens com.example to javafx.fxml;

    exports com.example;
}
