module com.joshuamc1.inventariomunicipal {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires activejdbc;
    requires org.jetbrains.annotations;
    requires java.sql;
    requires java.desktop;
    requires jbcrypt;


    opens com.joshuamc1.inventariomunicipal.controllers to javafx.fxml;

    opens com.joshuamc1.inventariomunicipal.models to javafx.fxml;

    exports com.joshuamc1.inventariomunicipal;
}