module com.example.mandelbrot {
    requires javafx.controls;
    requires javafx.fxml;
    requires mpj;
    requires java.base;
    requires log4j;
    requires java.desktop;


    opens com.example.mandelbrot to javafx.fxml;
    exports com.example.mandelbrot;
}