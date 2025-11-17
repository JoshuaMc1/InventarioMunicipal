module com.joshuamc1.inventariomunicipal {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires activejdbc;
    requires org.jetbrains.annotations;
    requires java.sql;
    requires java.desktop;
    requires jbcrypt;
    requires org.slf4j;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;


    opens com.joshuamc1.inventariomunicipal.controllers to javafx.fxml;
    opens com.joshuamc1.inventariomunicipal.controllers.dashboard to javafx.fxml;

    opens com.joshuamc1.inventariomunicipal.models to javafx.fxml, activejdbc;

    exports com.joshuamc1.inventariomunicipal;
    opens com.joshuamc1.inventariomunicipal.models.dto to activejdbc, javafx.fxml;
}