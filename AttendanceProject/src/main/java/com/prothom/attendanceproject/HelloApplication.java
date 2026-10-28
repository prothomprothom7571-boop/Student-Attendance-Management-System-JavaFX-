package com.prothom.attendanceproject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HelloApplication extends Application {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // Settings State
    private String presentIcon = "✅";
    private String absentIcon = "❌";

    public class Student {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final Map<String, SimpleStringProperty> dailyStatus = new HashMap<>();
        private final SimpleStringProperty percentage = new SimpleStringProperty("0.0%");

        public Student(String id, String name) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);

            for (String day : DAYS) {
                dailyStatus.put(day, new SimpleStringProperty(""));
            }
            updatePercentage();
        }

        public String getId() { return id.get(); }
        public void setId(String id) { this.id.set(id); }
        public SimpleStringProperty idProperty() { return id; }

        public String getName() { return name.get(); }
        public void setName(String name) { this.name.set(name); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getDayStatus(String day) { return dailyStatus.get(day).get(); }
        public void setDayStatus(String day, String status) {
            dailyStatus.get(day).set(status);
            updatePercentage();
        }
        public SimpleStringProperty dayStatusProperty(String day) { return dailyStatus.get(day); }

        public SimpleStringProperty percentageProperty() { return percentage; }

        public double getNumericPercentage() {
            String val = percentage.get().replace("%", "");
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        public void updatePercentage() {
            int presentCount = 0;
            int totalMarked = 0;

            for (String day : DAYS) {
                String status = dailyStatus.get(day).get();
                if (status.equals(presentIcon)) {
                    presentCount++;
                    totalMarked++;
                } else if (status.equals(absentIcon)) {
                    totalMarked++;
                }
            }

            if (totalMarked == 0) {
                percentage.set("0.0%");
            } else {
                double pct = ((double) presentCount / totalMarked) * 100;
                percentage.set(String.format("%.1f%%", pct));
            }
        }
    }

    private boolean isDarkMode = false;

    // Multi-Subject Mapping
    private final Map<String, ObservableList<Student>> subjectDataMap = new HashMap<>();
    private ObservableList<Student> currentStudentList = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;

    private final PieChart pieChart = new PieChart();
    private final Label analyticsTitleLabel = new Label("📈 Quick Analytics");
    private final Label presentSummaryLabel = new Label("Total Present: 0");
    private final Label absentSummaryLabel = new Label("Total Absent: 0");
    private final Label percentageSummaryLabel = new Label("Attendance Rate: 0.0%");
    private final ProgressBar overallProgressBar = new ProgressBar(0);

    private TableView<Student> table;
    private ComboBox<String> subjectDropdown;

    // Stopwatch Variables
    private Timeline stopwatchTimeline;
    private int stopwatchSeconds = 0;
    private Label stopwatchLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("⚡ Advanced Multi-Subject Attendance System");

        // --- MULTI-SUBJECT SETUP ---
        String defaultSubject = "Data Structures";
        subjectDropdown = new ComboBox<>();
        subjectDropdown.getItems().add(defaultSubject);
        subjectDropdown.setValue(defaultSubject);
        subjectDropdown.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-pref-width: 180px;");

        ObservableList<Student> defaultList = FXCollections.observableArrayList(
                new Student("10001", "Robert"),
                new Student("10002", "William"),
                new Student("10003", "Monica")
        );
        subjectDataMap.put(defaultSubject, defaultList);
        currentStudentList.addAll(defaultList);

        Button addSubjectBtn = new Button("➕ Add Subject");
        addSubjectBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        addSubjectBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Subject");
            dialog.setHeaderText("Add a new subject:");
            dialog.setContentText("Subject Name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(subName -> {
                String trimmed = subName.trim();
                if (!trimmed.isEmpty() && !subjectDataMap.containsKey(trimmed)) {
                    subjectDataMap.put(trimmed, FXCollections.observableArrayList());
                    subjectDropdown.getItems().add(trimmed);
                    subjectDropdown.setValue(trimmed);
                }
            });
        });

        subjectDropdown.setOnAction(e -> switchSubject(subjectDropdown.getValue()));

        // --- HEADER SECTION ---
        Label titleLabel = new Label("📊 Attendance Dashboard");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox subjectBox = new HBox(8, subjectDropdown, addSubjectBtn);
        subjectBox.setAlignment(Pos.CENTER_LEFT);

        Label dateTimeLabel = new Label();
        dateTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalTime currentTime = LocalTime.now();
            LocalDate currentDate = LocalDate.now();
            String formattedDateTime = String.format("📅 %s | ⏰ %s",
                    currentDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                    currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
            dateTimeLabel.setText(formattedDateTime);
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // --- STOPWATCH SETUP ---
        stopwatchLabel = new Label("⏱️ 00:00:00");
        stopwatchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f1c40f;");

        Button startStopwatchBtn = new Button("▶");
        startStopwatchBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button pauseStopwatchBtn = new Button("⏸");
        pauseStopwatchBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button resetStopwatchBtn = new Button("⏹");
        resetStopwatchBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        stopwatchTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            stopwatchSeconds++;
            int hours = stopwatchSeconds / 3600;
            int minutes = (stopwatchSeconds % 3600) / 60;
            int secs = stopwatchSeconds % 60;
            stopwatchLabel.setText(String.format("⏱️ %02d:%02d:%02d", hours, minutes, secs));
        }));
        stopwatchTimeline.setCycleCount(Timeline.INDEFINITE);

        startStopwatchBtn.setOnAction(e -> stopwatchTimeline.play());
        pauseStopwatchBtn.setOnAction(e -> stopwatchTimeline.pause());
        resetStopwatchBtn.setOnAction(e -> {
            stopwatchTimeline.stop();
            stopwatchSeconds = 0;
            stopwatchLabel.setText("⏱️ 00:00:00");
        });

        HBox stopwatchBox = new HBox(5, stopwatchLabel, startStopwatchBtn, pauseStopwatchBtn, resetStopwatchBtn);
        stopwatchBox.setAlignment(Pos.CENTER);

        ComboBox<String> weekSelector = new ComboBox<>();
        weekSelector.getItems().addAll("Week 1", "Week 2", "Week 3", "Week 4");
        weekSelector.setValue("Week 1");
        weekSelector.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        ToggleButton darkModeToggle = new ToggleButton("🌙 Dark Mode");
        darkModeToggle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerBox = new HBox(12, titleLabel, subjectBox, dateTimeLabel, stopwatchBox, headerSpacer, weekSelector, darkModeToggle);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(12, 18, 12, 18));
        // Changed background color to light green (#27ae60)
        headerBox.setStyle("-fx-background-color: #27ae60; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 3);");

        // --- SEARCH & TOOLBAR SECTION ---
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search student by ID or Name...");
        searchField.setPrefWidth(320);
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 15;");

        Button exportBtn = new Button("💾 Export CSV");
        exportBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");

        Button importBtn = new Button("📂 Import CSV");
        importBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");

        HBox toolBar = new HBox(15, searchField, exportBtn, importBtn);
        toolBar.setPadding(new Insets(10, 18, 5, 18));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        // --- TABLE SETUP ---
        table = new TableView<>();
        table.setEditable(true);
        table.setStyle("-fx-font-size: 15px;");

        filteredData = new FilteredList<>(currentStudentList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(student -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return student.getName().toLowerCase().contains(lowerCaseFilter) ||
                        student.getId().toLowerCase().contains(lowerCaseFilter);
            });
        });
        table.setItems(filteredData);

        TableColumn<Student, String> colId = new TableColumn<>("Student ID");
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colId.setCellFactory(TextFieldTableCell.forTableColumn());
        colId.setOnEditCommit(e -> e.getRowValue().setId(e.getNewValue()));
        colId.setPrefWidth(110);

        TableColumn<Student, String> colName = new TableColumn<>("Student Name");
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        colName.setPrefWidth(160);

        table.getColumns().addAll(colId, colName);

        for (String day : DAYS) {
            TableColumn<Student, String> dayCol = new TableColumn<>(day);
            dayCol.setPrefWidth(75);
            dayCol.setCellValueFactory(cellData -> cellData.getValue().dayStatusProperty(day));

            dayCol.setCellFactory(tc -> new TableCell<Student, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                        if (item.equals(presentIcon)) {
                            setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else if (item.equals(absentIcon)) {
                            setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else {
                            setStyle("-fx-font-size: 15px;");
                        }
                    }
                }

                {
                    setOnMouseClicked(event -> {
                        if (!isEmpty()) {
                            Student student = getTableView().getItems().get(getIndex());
                            String currentStatus = student.getDayStatus(day);

                            if (currentStatus.isEmpty()) {
                                student.setDayStatus(day, presentIcon);
                            } else if (currentStatus.equals(presentIcon)) {
                                student.setDayStatus(day, absentIcon);
                            } else {
                                student.setDayStatus(day, "");
                            }
                            updateAnalyticsForSelectedStudent();
                            table.refresh();
                        }
                    });
                }
            });

            table.getColumns().add(dayCol);
        }

        TableColumn<Student, String> colPct = new TableColumn<>("Attendance %");
        colPct.setCellValueFactory(cellData -> cellData.getValue().percentageProperty());
        colPct.setPrefWidth(120);
        colPct.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-size: 15px;");
        table.getColumns().add(colPct);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateAnalyticsForSelectedStudent();
        });

        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setPrefHeight(38);
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.isEmpty()) {
                    showAddStudentDialog();
                }
            });
            return row;
        });

        // --- DASHBOARD / PIE CHART PANEL ---
        pieChart.setPrefSize(260, 200);
        pieChart.setLegendVisible(false);
        pieChart.setAnimated(true);

        analyticsTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        presentSummaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        absentSummaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");
        percentageSummaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        overallProgressBar.setPrefWidth(240);
        overallProgressBar.setStyle("-fx-accent: #27ae60;");

        VBox statsBox = new VBox(10, analyticsTitleLabel, presentSummaryLabel, absentSummaryLabel, percentageSummaryLabel, overallProgressBar, pieChart);
        statsBox.setPadding(new Insets(15));
        statsBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        statsBox.setPrefWidth(290);

        HBox centerContent = new HBox(12, table, statsBox);
        HBox.setHgrow(table, Priority.ALWAYS);
        centerContent.setPadding(new Insets(0, 18, 0, 18));

        // --- BOTTOM INPUT FORM SECTION ---
        TextField idInput = new TextField();
        idInput.setPromptText("Student ID");
        idInput.setStyle("-fx-font-size: 13px;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Student Name");
        nameInput.setStyle("-fx-font-size: 13px;");

        Button addButton = new Button("Add Student");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");

        addButton.setOnAction(e -> {
            if (!idInput.getText().trim().isEmpty() && !nameInput.getText().trim().isEmpty()) {
                currentStudentList.add(new Student(idInput.getText().trim(), nameInput.getText().trim()));
                idInput.clear();
                nameInput.clear();
                updateAnalyticsForSelectedStudent();
            }
        });

        deleteButton.setOnAction(e -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentStudentList.remove(selected);
                updateAnalyticsForSelectedStudent();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Select a student to delete!");
                alert.showAndWait();
            }
        });

        HBox inputBox = new HBox(12, idInput, nameInput, addButton, deleteButton);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        // --- BOTTOM SETTINGS PANEL ---
        TitledPane settingsPane = new TitledPane();
        settingsPane.setText("⚙️ Customization Settings");
        settingsPane.setExpanded(false);

        ComboBox<String> iconStyleCombo = new ComboBox<>();
        iconStyleCombo.getItems().addAll("✅ / ❌ (Emojis)", "P / A (Text)", "Present / Absent");
        iconStyleCombo.setValue("✅ / ❌ (Emojis)");

        iconStyleCombo.setOnAction(e -> {
            String selected = iconStyleCombo.getValue();
            if (selected.startsWith("P")) {
                presentIcon = "P";
                absentIcon = "A";
            } else if (selected.startsWith("Present")) {
                presentIcon = "Present";
                absentIcon = "Absent";
            } else {
                presentIcon = "✅";
                absentIcon = "❌";
            }
            for (Student s : currentStudentList) {
                s.updatePercentage();
            }
            table.refresh();
        });

        HBox settingsContent = new HBox(15, new Label("Status Icon Style:"), iconStyleCombo);
        settingsContent.setAlignment(Pos.CENTER_LEFT);
        settingsContent.setPadding(new Insets(10));
        settingsPane.setContent(settingsContent);

        VBox bottomContainer = new VBox(10, inputBox, settingsPane);
        bottomContainer.setPadding(new Insets(10, 18, 10, 18));

        // --- MAIN LAYOUT ---
        VBox mainLayout = new VBox(10, headerBox, toolBar, centerContent, bottomContainer);
        Scene scene = new Scene(mainLayout, 1350, 720);

        scene.setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();
            boolean clickedInsideRow = false;

            while (target != null) {
                if (target instanceof TableRow && !((TableRow<?>) target).isEmpty()) {
                    clickedInsideRow = true;
                    break;
                }
                target = target.getParent();
            }

            if (!clickedInsideRow) {
                table.getSelectionModel().clearSelection();
                updateAnalyticsForSelectedStudent();
            }
        });

        exportBtn.setOnAction(e -> exportToCSV(primaryStage));
        importBtn.setOnAction(e -> importFromCSV(primaryStage));

        darkModeToggle.setOnAction(e -> {
            isDarkMode = darkModeToggle.isSelected();
            if (isDarkMode) {
                darkModeToggle.setText("☀️ Light Mode");
                scene.getRoot().setStyle("-fx-base: #2b2b2b; -fx-background-color: #1e1e1e;");
                headerBox.setStyle("-fx-background-color: #3c3f41;");
                statsBox.setStyle("-fx-border-color: #555555; -fx-background-color: #2a2a2a;");
            } else {
                darkModeToggle.setText("🌙 Dark Mode");
                scene.getRoot().setStyle("");
                headerBox.setStyle("-fx-background-color: #27ae60;");
                statsBox.setStyle("-fx-border-color: #e0e0e0; -fx-background-color: #ffffff;");
            }
        });

        updateAnalyticsForSelectedStudent();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void switchSubject(String subjectName) {
        if (subjectName == null || !subjectDataMap.containsKey(subjectName)) return;

        ObservableList<Student> selectedList = subjectDataMap.get(subjectName);
        currentStudentList.setAll(selectedList);

        filteredData = new FilteredList<>(currentStudentList, p -> true);
        table.setItems(filteredData);

        updateAnalyticsForSelectedStudent();
    }

    private void updateAnalyticsForSelectedStudent() {
        Student selected = table.getSelectionModel().getSelectedItem();

        if (selected != null) {
            analyticsTitleLabel.setText("👤 Student: " + selected.getName());

            int presentCount = 0;
            int absentCount = 0;

            for (String day : DAYS) {
                String status = selected.getDayStatus(day);
                if (status.equals(presentIcon)) presentCount++;
                else if (status.equals(absentIcon)) absentCount++;
            }

            presentSummaryLabel.setText("Present Days: " + presentCount);
            absentSummaryLabel.setText("Absent Days: " + absentCount);
            percentageSummaryLabel.setText("Attendance Rate: " + selected.percentageProperty().get());

            double pct = selected.getNumericPercentage() / 100.0;
            overallProgressBar.setProgress(pct);

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Present", presentCount),
                    new PieChart.Data("Absent", absentCount)
            );
            pieChart.setData(pieChartData);

        } else {
            analyticsTitleLabel.setText("📈 Class: " + subjectDropdown.getValue());

            int totalPresent = 0;
            int totalAbsent = 0;

            for (Student student : currentStudentList) {
                for (String day : DAYS) {
                    String status = student.getDayStatus(day);
                    if (status.equals(presentIcon)) totalPresent++;
                    else if (status.equals(absentIcon)) totalAbsent++;
                }
            }

            presentSummaryLabel.setText("Total Class Present: " + totalPresent);
            absentSummaryLabel.setText("Total Class Absent: " + totalAbsent);

            int totalMarked = totalPresent + totalAbsent;
            double classPct = totalMarked == 0 ? 0.0 : ((double) totalPresent / totalMarked) * 100;
            percentageSummaryLabel.setText(String.format("Overall Rate: %.1f%%", classPct));

            overallProgressBar.setProgress(classPct / 100.0);

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Present", totalPresent),
                    new PieChart.Data("Absent", totalAbsent)
            );
            pieChart.setData(pieChartData);
        }
    }

    private void showAddStudentDialog() {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Add New Student");
        dialog.setHeaderText("Enter Details:");

        ButtonType addBtnType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField idField = new TextField();
        idField.setStyle("-fx-font-size: 14px;");
        TextField nameField = new TextField();
        nameField.setStyle("-fx-font-size: 14px;");

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtnType && !idField.getText().trim().isEmpty() && !nameField.getText().trim().isEmpty()) {
                return new Student(idField.getText().trim(), nameField.getText().trim());
            }
            return null;
        });

        Optional<Student> res = dialog.showAndWait();
        res.ifPresent(s -> {
            currentStudentList.add(s);
            updateAnalyticsForSelectedStudent();
        });
    }

    private void exportToCSV(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Attendance CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Subject," + subjectDropdown.getValue() + "\n");
                writer.write("ID,Name,Mon,Tue,Wed,Thu,Fri,Sat,Sun\n");
                for (Student s : currentStudentList) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                            s.getId(), s.getName(),
                            s.getDayStatus("Mon"), s.getDayStatus("Tue"), s.getDayStatus("Wed"),
                            s.getDayStatus("Thu"), s.getDayStatus("Fri"), s.getDayStatus("Sat"),
                            s.getDayStatus("Sun")));
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Data successfully saved to CSV!");
                alert.showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void importFromCSV(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Attendance CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                String loadedSubject = "Imported Subject";

                if (line != null && line.startsWith("Subject,")) {
                    String[] subData = line.split(",", 2);
                    if (subData.length > 1) {
                        loadedSubject = subData[1].trim();
                    }
                    line = reader.readLine();
                }

                ObservableList<Student> importedList = FXCollections.observableArrayList();

                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length >= 9) {
                        Student s = new Student(data[0], data[1]);
                        for (int i = 0; i < DAYS.length; i++) {
                            s.setDayStatus(DAYS[i], data[i + 2]);
                        }
                        importedList.add(s);
                    }
                }

                subjectDataMap.put(loadedSubject, importedList);
                if (!subjectDropdown.getItems().contains(loadedSubject)) {
                    subjectDropdown.getItems().add(loadedSubject);
                }
                subjectDropdown.setValue(loadedSubject);
                switchSubject(loadedSubject);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Data imported for: " + loadedSubject);
                alert.showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}