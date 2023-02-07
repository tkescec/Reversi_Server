module hr.reversi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;
    requires java.xml;


    opens hr.reversi to javafx.fxml;
    exports hr.reversi;
    exports hr.reversi.controller;
    opens hr.reversi.controller to javafx.fxml;

    exports hr.reversi.rmi.server to
            java.rmi;
}