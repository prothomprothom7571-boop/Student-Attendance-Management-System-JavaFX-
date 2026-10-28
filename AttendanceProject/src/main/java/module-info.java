module com.prothom.attendanceproject {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.prothom.attendanceproject to javafx.fxml;
    exports com.prothom.attendanceproject;
}