module com.example.tpo3 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.tpo3 to javafx.fxml;
    exports com.example.tpo3;
    exports Admin;
}