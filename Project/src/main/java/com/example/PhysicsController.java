package com.example;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import com.example.sim.BallProjectile;
import com.example.sim.GroundObject;
import com.example.sim.ProjectileObject;
import com.example.sim.PumpkinProjectile;
import com.example.sim.SimObject;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.paint.Color;

public class PhysicsController implements Initializable {

    // === PHYSICS CONSTANTS (easy to modify for experimentation) ===

    /** Default gravity strength in m/s² (positive magnitude) */

    private static final double DEFAULT_GRAVITY = 9.8;

    /** Ground width in meters */

    private static final double GROUND_WIDTH = 20.0;

    /** Ground height in meters */

    private static final double GROUND_HEIGHT = 1.0;

    /** Ground Y position in meters (negative = below center) */

    private static final double GROUND_Y_POSITION = -3.0;

    /** Pixels per meter for coordinate conversion */

    private static final double PIXELS_PER_METER = 50.0;

    private static final String SAVE_FILE = "physics_experiments.csv";

    private static final double MIN_ANGLE_DEGREES = 0;
    private static final double MAX_ANGLE_DEGREES = 90;

    private static final double MIN_GRAVITY = 0.0;
    private static final double MAX_GRAVITY = 20.0;

    private static final double MIN_AIR_RESISTANCE = 0.0;
    private static final double MAX_AIR_RESISTANCE = 5.0;

    // === JavaFX COMPONENTS ===

    @FXML
    private Canvas canvas;

    // Physics world - this is where all the physics simulation happens

    private World world;

    private GroundObject groundObject;
    private ProjectileObject projectileObject;
    private final List<SimObject> simObjects = new ArrayList<>();

    // Animation timer for continuous updates

    private AnimationTimer gameLoop;

    // Timing variables for physics updates

    private long lastUpdateTime = 0;

    @FXML
    private TextField angleField;

    @FXML
    private Label angleErrorLabel;

    @FXML
    private Button launchButton;

    @FXML
    private Button resetButton;

    @FXML
    private Label speedLabel;

    @FXML
    private Label labelHeight;

    @FXML
    private Label labelMaxHeight;

    @FXML
    private Label labelRange;

    @FXML
    private Label labelTheoreticalRange;

    @FXML
    private Label labelRangeDifference;

    @FXML
    private Label labelDroppedRange;

    @FXML
    private Slider speedSlider;

    @FXML
    private Label massLabel;

    @FXML
    private Slider massSlider;

    @FXML
    private Slider gravitySlider;

    @FXML
    private Label gravityLabel;

    @FXML
    private Slider airResistanceSlider;

    @FXML
    private Label airResistanceLabel;

    @FXML
    private ComboBox<String> projectileTypeCombo;

    @FXML
    private Button saveButton;

    @FXML
    private Button loadButton;

    @FXML
    private ListView<String> experimentListView;

    @FXML
    private ComboBox<String> sortByCombo;

    @FXML
    private Button sortButton;

    @FXML
    private TextField searchRangeField;

    @FXML
    private Button searchButton;

    @FXML
    private Button deleteButton;

    private ObservableList<ExperimentRecord> savedExperiments = FXCollections.observableArrayList();
    private ObservableList<String> experimentDisplayList = FXCollections.observableArrayList();

    private double projectileStartX = -8;
    private double projectileStartY = -2;
    private Double landedRange = null;
    private boolean hasBeenLaunched = false;
    private boolean hasBeenAirborne = false;
    private static final double GROUND_CONTACT_TOLERANCE = 0.1;

    private double maxHeightMeters = 0.0;

    private Double lastLaunchSpeed = null;
    private Double lastLaunchAngle = null;
    private double theoreticalRangeMeters = 0.0;

    private List<Vector2> trajectoryTrace = new ArrayList<>();
    private static final double TRACE_POINT_RADIUS = 3.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("PhysicsController initialized!");
        System.out.println("Canvas size: " + canvas.getWidth() + "x" + canvas.getHeight());
        // Step 1: Create the physics world
        setupPhysicsWorld();

        // Step 2: Create ground & projectile objects
        createSimObjects();

        // Step 4: Set up mouse interaction
        setupUIControls();

        // Step 5: Draw the initial state
        render();

        // Step 6: Start the animation loop
        startGameLoop();
    }

    /**
     * * Creates and configures the physics world
     * 
     */

    private void setupPhysicsWorld() {
        // Create a new physics world with default settings
        world = new World();

        // Set gravity (pointing downward)
        // In Dyn4j, positive Y is up, so gravity should be negative
        world.setGravity(new Vector2(0.0, -DEFAULT_GRAVITY));

        System.out.println("Physics world created with gravity: " + world.getGravity());
    }

    private void createSimObjects() {
        groundObject = new GroundObject(GROUND_WIDTH, GROUND_HEIGHT, GROUND_Y_POSITION);
        projectileObject = new BallProjectile(projectileStartX, projectileStartY);

        simObjects.clear();
        simObjects.add(groundObject);
        simObjects.add(projectileObject);

        for (SimObject obj : simObjects) {
            obj.addToWorld(world);
        }

        projectileObject.setMassKg(massSlider.getValue());
        applyAirResistanceToProjectile();
        System.out.println("Ground created at position: " + groundObject.getBody().getTransform().getTranslation());
    }

    private void setupUIControls() {
        projectileTypeCombo.setItems(FXCollections.observableArrayList("Ball", "Pumpkin"));
        projectileTypeCombo.getSelectionModel().select("Ball");
        projectileTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            switchProjectile(newValue);
        });

        speedSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    speedLabel.setText(String.format("%.1f m/s", newValue.doubleValue()));
                    updateTheoreticalRangeUI();
                });
        massSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    massLabel.setText(String.format("%.1f kg", newValue.doubleValue()));
                    if (projectileObject != null) {
                        projectileObject.setMassKg(newValue.doubleValue());
                    }
                });

        if (gravitySlider != null) {
            gravitySlider.setMin(MIN_GRAVITY);
            gravitySlider.setMax(MAX_GRAVITY);
            gravitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyGravityFromSlider();
                updateTheoreticalRangeUI();
            });
        }

        if (airResistanceSlider != null) {
            airResistanceSlider.setMin(MIN_AIR_RESISTANCE);
            airResistanceSlider.setMax(MAX_AIR_RESISTANCE);
            airResistanceSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyAirResistanceFromSlider();
            });
        }
        resetButton.setOnAction(event -> resetProjectile());
        launchButton.setOnAction(event -> launchProjectile());
        saveButton.setOnAction(event -> saveCurrentExperiment());
        loadButton.setOnAction(event -> loadExperimentsFromFile());

        setupAngleValidation();

        applyGravityFromSlider();
        applyAirResistanceFromSlider();

        // Setup sort combo box
        sortByCombo.setItems(
                FXCollections.observableArrayList("Range", "Speed", "Angle", "Mass", "Gravity", "Air Resistance",
                        "Theoretical Range"));
        sortByCombo.getSelectionModel().select("Range");
        sortButton.setOnAction(event -> sortExperiments());
        searchButton.setOnAction(event -> searchExperimentByRange());
        deleteButton.setOnAction(event -> deleteSelectedExperiment());
        deleteButton.setDisable(true);

        experimentListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex == null || newIndex.intValue() < 0 || newIndex.intValue() >= savedExperiments.size()) {
                if (deleteButton != null) {
                    deleteButton.setDisable(true);
                }
                return;
            }

            if (deleteButton != null) {
                deleteButton.setDisable(false);
            }
            applyExperimentToControls(savedExperiments.get(newIndex.intValue()));
        });

        experimentListView.setItems(experimentDisplayList);
    }

    private void deleteSelectedExperiment() {
        if (savedExperiments.isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No Experiments");
            alert.setHeaderText(null);
            alert.setContentText("There are no saved experiments to delete.");
            alert.showAndWait();
            return;
        }

        int index = experimentListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= savedExperiments.size()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Select an experiment from the list first.");
            alert.showAndWait();
            return;
        }

        savedExperiments.remove(index);
        experimentDisplayList.remove(index);
        experimentListView.getSelectionModel().clearSelection();

        if (deleteButton != null) {
            deleteButton.setDisable(true);
        }

        rewriteExperimentsCsvFile();

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Deleted");
        alert.setHeaderText(null);
        alert.setContentText("Experiment deleted and CSV updated.");
        alert.showAndWait();
    }

    private void rewriteExperimentsCsvFile() {
        Path savePath = Path.of(SAVE_FILE);

        try {
            // Always rewrite the file so it matches the current in-app list.
            // Keep a header row for clarity.
            Files.writeString(savePath, "speed,angle,mass,range,maxHeight,gravity,airResistance\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            for (int i = 0; i < savedExperiments.size(); i++) {
                ExperimentRecord record = savedExperiments.get(i);
                String line = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n", record.getSpeed(),
                        record.getAngle(), record.getMass(), record.getLandedRange(), record.getMaxHeight(),
                        record.getGravity(), record.getAirResistance());
                Files.writeString(savePath, line, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Delete failed");
            alert.setHeaderText(null);
            alert.setContentText("Could not update CSV file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupAngleValidation() {
        angleField.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }
            if (newText.matches("\\d{0,3}(\\.\\d{0,4})?")) {
                return change;
            }
            return null;
        }));

        angleField.textProperty().addListener((obs, oldValue, newValue) -> updateAngleValidationUI(false));
        updateAngleValidationUI(false);

        updateTheoreticalRangeUI();
    }

    private void applyGravityFromSlider() {
        double g = DEFAULT_GRAVITY;

        if (gravitySlider != null) {
            g = gravitySlider.getValue();
        }

        if (g < MIN_GRAVITY) {
            g = MIN_GRAVITY;
        }
        if (g > MAX_GRAVITY) {
            g = MAX_GRAVITY;
        }

        if (gravityLabel != null) {
            gravityLabel.setText(String.format("%.1f m/s²", g));
        }

        if (world != null) {
            world.setGravity(new Vector2(0.0, -g));
        }
    }

    private void applyAirResistanceFromSlider() {
        double damping = 0.0;

        if (airResistanceSlider != null) {
            damping = airResistanceSlider.getValue();
        }

        if (damping < MIN_AIR_RESISTANCE) {
            damping = MIN_AIR_RESISTANCE;
        }
        if (damping > MAX_AIR_RESISTANCE) {
            damping = MAX_AIR_RESISTANCE;
        }

        if (airResistanceLabel != null) {
            airResistanceLabel.setText(String.format("%.1f", damping));
        }

        applyAirResistanceToProjectile();
    }

    private void applyAirResistanceToProjectile() {
        if (projectileObject == null) {
            return;
        }
        if (projectileObject.getBody() == null) {
            return;
        }

        double damping = 0.0;
        if (airResistanceSlider != null) {
            damping = airResistanceSlider.getValue();
        }
        if (damping < MIN_AIR_RESISTANCE) {
            damping = MIN_AIR_RESISTANCE;
        }
        if (damping > MAX_AIR_RESISTANCE) {
            damping = MAX_AIR_RESISTANCE;
        }

        projectileObject.getBody().setLinearDamping(damping);
        projectileObject.getBody().setAtRest(false);
    }

    private double getCurrentGravityMagnitude() {
        if (world == null) {
            return DEFAULT_GRAVITY;
        }

        Vector2 g = world.getGravity();
        if (g == null) {
            return DEFAULT_GRAVITY;
        }

        return Math.abs(g.y);
    }

    private void updateAngleValidationUI(boolean showAlert) {
        String message = validateAngleText(angleField.getText());
        boolean valid = (message == null);

        if (angleErrorLabel != null) {
            angleErrorLabel.setManaged(!valid);
            angleErrorLabel.setVisible(!valid);
            angleErrorLabel.setText(valid ? "" : message);
        }

        if (launchButton != null) {
            launchButton.setDisable(!valid);
        }
        if (saveButton != null) {
            // You can only save after landing anyway, but this prevents saving with a bad
            // edited angle.
            saveButton.setDisable(!valid);
        }

        if (showAlert && !valid) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Invalid Angle");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        updateTheoreticalRangeUI();
    }

    private double computeTheoreticalRangeMeters(double speedMetersPerSecond, double angleDegrees) {
        double g = getCurrentGravityMagnitude();
        if (g <= 0) {
            return 0.0;
        }

        double angleRadians = Math.toRadians(angleDegrees);
        double range = (speedMetersPerSecond * speedMetersPerSecond * Math.sin(2.0 * angleRadians)) / g;
        return Math.max(0.0, range);
    }

    private void updateTheoreticalRangeUI() {
        double range = 0.0;

        if (hasBeenLaunched && lastLaunchSpeed != null && lastLaunchAngle != null) {
            range = theoreticalRangeMeters;
        } else {
            String validationMessage = validateAngleText(angleField.getText());
            if (validationMessage == null) {
                try {
                    double speed = speedSlider.getValue();
                    double angle = Double.parseDouble(angleField.getText().trim());
                    range = computeTheoreticalRangeMeters(speed, angle);
                } catch (NumberFormatException e) {
                    range = 0.0;
                }
            }
        }

        if (labelTheoreticalRange != null) {
            labelTheoreticalRange.setText(String.format("%.2f m", range));
        }

        if (labelRangeDifference != null) {
            if (landedRange == null) {
                labelRangeDifference.setText("--");
            } else {
                double diff = landedRange.doubleValue() - range;
                if (range > 0.0001) {
                    double percent = (diff / range) * 100.0;
                    labelRangeDifference.setText(String.format("%.2f m (%.1f%%)", diff, percent));
                } else {
                    labelRangeDifference.setText(String.format("%.2f m", diff));
                }
            }
        }
    }

    private String validateAngleText(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            return "Enter a launch angle.";
        }

        double angle;
        try {
            angle = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return "Angle must be a valid number.";
        }

        if (Double.isNaN(angle) || Double.isInfinite(angle)) {
            return "Angle must be a normal number.";
        }
        if (angle < MIN_ANGLE_DEGREES || angle > MAX_ANGLE_DEGREES) {
            return String.format("Angle must be between %.0f° and %.0f°.", MIN_ANGLE_DEGREES, MAX_ANGLE_DEGREES);
        }
        return null;
    }

    private void switchProjectile(String projectileType) {
        if (projectileObject != null) {
            projectileObject.removeFromWorld(world);
            simObjects.remove(projectileObject);
        }

        if ("Pumpkin".equalsIgnoreCase(projectileType)) {
            projectileObject = new PumpkinProjectile(projectileStartX, projectileStartY);
        } else {
            projectileObject = new BallProjectile(projectileStartX, projectileStartY);
        }

        projectileObject.addToWorld(world);
        projectileObject.setMassKg(massSlider.getValue());
        applyAirResistanceToProjectile();
        simObjects.add(projectileObject);

        resetProjectile();
    }

    private void applyExperimentToControls(ExperimentRecord record) {
        projectileObject.reset();
        landedRange = null;
        hasBeenLaunched = false;
        hasBeenAirborne = false;
        trajectoryTrace.clear();
        maxHeightMeters = record.getMaxHeight();

        if (gravitySlider != null) {
            gravitySlider.setValue(record.getGravity());
        }
        if (airResistanceSlider != null) {
            airResistanceSlider.setValue(record.getAirResistance());
        }
        applyGravityFromSlider();
        applyAirResistanceFromSlider();

        lastLaunchSpeed = record.getSpeed();
        lastLaunchAngle = record.getAngle();
        theoreticalRangeMeters = computeTheoreticalRangeMeters(record.getSpeed(), record.getAngle());

        speedSlider.setValue(record.getSpeed());
        massSlider.setValue(record.getMass());
        angleField.setText(String.format("%.1f", record.getAngle()));

        labelDroppedRange.setText(String.format("%.2f m", record.getLandedRange()));
        labelHeight.setText("0.00 m");
        if (labelMaxHeight != null) {
            labelMaxHeight.setText(String.format("%.2f m", record.getMaxHeight()));
        }
        labelRange.setText("0.00 m");

        if (labelTheoreticalRange != null) {
            labelTheoreticalRange.setText(String.format("%.2f m", theoreticalRangeMeters));
        }
        if (labelRangeDifference != null) {
            double diff = record.getLandedRange() - theoreticalRangeMeters;
            if (theoreticalRangeMeters > 0.0001) {
                double percent = (diff / theoreticalRangeMeters) * 100.0;
                labelRangeDifference.setText(String.format("%.2f m (%.1f%%)", diff, percent));
            } else {
                labelRangeDifference.setText(String.format("%.2f m", diff));
            }
        }
    }

    private void loadExperimentsFromFile() {
        Path savePath = Path.of(SAVE_FILE);

        if (!Files.exists(savePath)) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No Saved Experiments");
            alert.setContentText("No CSV file found yet. Save an experiment first.");
            alert.showAndWait();
            return;
        }

        try {
            List<String> lines = Files.readAllLines(savePath);
            savedExperiments.clear();
            experimentDisplayList.clear();
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("speed")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 4 && parts.length != 5 && parts.length != 6 && parts.length != 7) {
                    continue;
                }

                try {
                    double speed = Double.parseDouble(parts[0]);
                    double angle = Double.parseDouble(parts[1]);
                    double mass = Double.parseDouble(parts[2]);
                    double range = Double.parseDouble(parts[3]);

                    double maxHeight = 0.0;
                    if (parts.length >= 5) {
                        maxHeight = Double.parseDouble(parts[4]);
                    }

                    double gravity = DEFAULT_GRAVITY;
                    double airResistance = 0.0;
                    if (parts.length >= 6) {
                        gravity = Double.parseDouble(parts[5]);
                    }
                    if (parts.length >= 7) {
                        airResistance = Double.parseDouble(parts[6]);
                    }

                    ExperimentRecord record = new ExperimentRecord(speed, angle, mass, range, maxHeight, gravity,
                            airResistance);
                    savedExperiments.add(record);
                    experimentDisplayList.add(formatExperiment(record));
                } catch (NumberFormatException e) {
                    System.out.println("Cannot convert line: " + line + " to record!");
                }
            }

            if (experimentDisplayList.isEmpty()) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("No valid Records");
                alert.setContentText("The CSV file has no valid experiments to load.");
                alert.showAndWait();
            }

        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Load failed");
            alert.setContentText("Could not read experiments from CSV: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void saveCurrentExperiment() {
        if (landedRange == null) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Cannot Save Experiment");
            alert.setHeaderText(null);
            alert.setContentText("Please launch the projectile and let it land before saving the experiment.");
            alert.showAndWait();
            return;
        }
        double speed = speedSlider.getValue();
        String angleString = angleField.getText().trim();
        double mass = massSlider.getValue();
        try {
            double angle = Double.parseDouble(angleString);
            double gravity = getCurrentGravityMagnitude();
            double airResistance = 0.0;
            if (airResistanceSlider != null) {
                airResistance = airResistanceSlider.getValue();
            }

            ExperimentRecord record = new ExperimentRecord(speed, angle, mass, landedRange, maxHeightMeters, gravity,
                    airResistance);
            savedExperiments.add(record);
            experimentDisplayList.add(formatExperiment(record));
            saveExperimentToCsv(record);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Invalid Angle");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid number for the launch angle.");
            alert.showAndWait();
        }
    }

    private String formatExperiment(ExperimentRecord record) {
        return String.format(
                "Speed: %.1f m/s, Angle: %.1f°, Mass: %.1f kg, Range: %.2f m, Max Height: %.2f m, g: %.1f, air: %.1f",
                record.getSpeed(), record.getAngle(), record.getMass(), record.getLandedRange(), record.getMaxHeight(),
                record.getGravity(), record.getAirResistance());
    }

    public void saveExperimentToCsv(ExperimentRecord record) {
        String line = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n", record.getSpeed(), record.getAngle(),
                record.getMass(), record.getLandedRange(), record.getMaxHeight(), record.getGravity(),
                record.getAirResistance());

        try {
            Path savePath = Path.of(SAVE_FILE);
            if (!Files.exists(savePath)) {
                Files.writeString(savePath, "speed,angle,mass,range,maxHeight,gravity,airResistance\n");
            }
            Files.writeString(savePath, line, StandardOpenOption.APPEND);
        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Save failed");
            alert.setContentText("Cannot save: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void resetProjectile() {
        projectileObject.reset();
        landedRange = null;
        hasBeenLaunched = false;
        hasBeenAirborne = false;
        trajectoryTrace.clear();
        maxHeightMeters = 0.0;

        lastLaunchSpeed = null;
        lastLaunchAngle = null;
        theoreticalRangeMeters = 0.0;

        labelDroppedRange.setText("-- m");
        labelHeight.setText("0.00 m");
        if (labelMaxHeight != null) {
            labelMaxHeight.setText("0.00 m");
        }
        labelRange.setText("0.00 m");

        updateTheoreticalRangeUI();
    }

    private void launchProjectile() {
        double speed = speedSlider.getValue();
        String angleString = angleField.getText().trim();
        try {
            double angle = Double.parseDouble(angleString);

            landedRange = null;
            hasBeenLaunched = true;
            hasBeenAirborne = false;
            maxHeightMeters = 0.0;

            lastLaunchSpeed = speed;
            lastLaunchAngle = angle;
            theoreticalRangeMeters = computeTheoreticalRangeMeters(speed, angle);

            labelDroppedRange.setText("-- m");
            if (labelMaxHeight != null) {
                labelMaxHeight.setText("0.00 m");
            }

            if (labelTheoreticalRange != null) {
                labelTheoreticalRange.setText(String.format("%.2f m", theoreticalRangeMeters));
            }
            if (labelRangeDifference != null) {
                labelRangeDifference.setText("--");
            }

            projectileObject.launch(speed, angle);

        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * * Starts the game loop that continuously updates physics and renders the
     * scene
     * 
     */

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long currentTime) {
                // Calculate time elapsed since last update (in seconds)
                if (lastUpdateTime == 0) {
                    lastUpdateTime = currentTime;
                    return;
                }

                double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // Convert nanoseconds to seconds
                lastUpdateTime = currentTime;

                // Limit delta time to prevent large jumps (max 1/30 second)
                deltaTime = Math.min(deltaTime, 1.0 / 30.0);

                // Update physics
                updatePhysics(deltaTime);

                // Render the scene
                render();
            }
        };

        // Start the animation timer
        gameLoop.start();
    }

    /**
     * * Updates the physics simulation
     * *
     * * @param deltaTime Time elapsed since last update in seconds
     * 
     */

    private void updatePhysics(double deltaTime) {
        // Update the physics world
        // The step method advances the simulation by the given time
        world.update(deltaTime);
    }

    /**
     * * Renders the physics world to the canvas
     * * This method converts world coordinates to screen coordinates and draws
     * all
     * * objects
     * 
     */

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear the canvas with a light blue background (like sky)
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (hasBeenLaunched && landedRange == null) {
            Vector2 position = projectileObject.getPosition();
            trajectoryTrace.add(position.copy());
        }

        drawTrajectoryTrace(gc);

        for (SimObject obj : simObjects) {
            obj.render(gc, canvas, PIXELS_PER_METER);
        }

        Vector2 position = projectileObject.getPosition();
        double groundTop = groundObject.getTopY();
        double height = Math.max(0.0, position.y - groundTop - projectileObject.getRadiusMeters());
        double range = Math.max(0.0, position.x - projectileStartX);

        if (hasBeenLaunched && landedRange == null) {
            if (height > maxHeightMeters) {
                maxHeightMeters = height;
            }
        }

        labelHeight.setText(String.format(("%.2f m"), height));
        if (labelMaxHeight != null) {
            labelMaxHeight.setText(String.format("%.2f m", maxHeightMeters));
        }
        labelRange.setText(String.format(("%.2f m"), range));

        if (hasBeenLaunched && height > 0.5) {
            hasBeenAirborne = true;
        }

        Vector2 velocity = projectileObject.getVelocity();
        if (landedRange == null && hasLanded(position, velocity)) {
            landedRange = range;
        }
        if (landedRange != null) {
            labelDroppedRange.setText(String.format("%.2f m", landedRange));
        }

        updateTheoreticalRangeUI();
    }

    private void drawTrajectoryTrace(GraphicsContext gc) {
        if (trajectoryTrace.isEmpty()) {
            return;
        }

        for (int i = 0; i < trajectoryTrace.size(); i++) {
            Vector2 pos = trajectoryTrace.get(i);

            double screenX = (canvas.getWidth() / 2) + (pos.x * PIXELS_PER_METER);
            double screenY = (canvas.getHeight() / 2) - (pos.y * PIXELS_PER_METER);

            double opacity = 0.3 + (0.7 * i / trajectoryTrace.size());

            gc.setFill(Color.color(1, 0.0, 0.0, opacity));

            gc.fillOval(screenX - TRACE_POINT_RADIUS, screenY - TRACE_POINT_RADIUS,
                    TRACE_POINT_RADIUS * 2, TRACE_POINT_RADIUS * 2);
        }
    }

    private boolean hasLanded(Vector2 position, Vector2 velocity) {
        if (!hasBeenLaunched || !hasBeenAirborne) {
            return false;
        }
        double groundTop = groundObject.getTopY();
        double height = position.y - groundTop - projectileObject.getRadiusMeters();
        return height <= GROUND_CONTACT_TOLERANCE;
    }

    private void sortExperiments() {
        if (savedExperiments.isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No Experiments");
            alert.setHeaderText(null);
            alert.setContentText("No experiments to sort. Load or save some experiments first.");
            alert.showAndWait();
            return;
        }

        // Determine sort criteria from combo box
        String sortByString = sortByCombo.getValue();
        int sortBy = ExperimentSorter.SORT_BY_RANGE; // Default

        if ("Speed".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_SPEED;
        } else if ("Angle".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_ANGLE;
        } else if ("Mass".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_MASS;
        } else if ("Gravity".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_GRAVITY;
        } else if ("Air Resistance".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_AIR_RESISTANCE;
        } else if ("Theoretical Range".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_THEORETICAL_RANGE;
        }

        // Convert ObservableList to regular ArrayList for sorting
        ArrayList<ExperimentRecord> listToSort = new ArrayList<ExperimentRecord>();
        for (int i = 0; i < savedExperiments.size(); i++) {
            listToSort.add(savedExperiments.get(i));
        }

        // Sort using merge sort
        List<ExperimentRecord> sortedList = ExperimentSorter.mergeSort(listToSort, sortBy);

        // Update the saved experiments and display list
        savedExperiments.clear();
        experimentDisplayList.clear();

        for (int i = 0; i < sortedList.size(); i++) {
            ExperimentRecord record = sortedList.get(i);
            savedExperiments.add(record);
            experimentDisplayList.add(formatExperiment(record));
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Sort Complete");
        alert.setHeaderText(null);
        alert.setContentText("Experiments sorted by " + sortByString + " using Merge Sort.");
        alert.showAndWait();
    }

    private void searchExperimentByRange() {
        if (savedExperiments.isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No Experiments");
            alert.setHeaderText(null);
            alert.setContentText("No experiments to search. Load or save some experiments first.");
            alert.showAndWait();
            return;
        }

        String searchText = searchRangeField.getText().trim();
        if (searchText.isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Invalid Input");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a value to search for.");
            alert.showAndWait();
            return;
        }

        double targetValue;
        try {
            targetValue = Double.parseDouble(searchText);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Invalid Number");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid number.");
            alert.showAndWait();
            return;
        }

        String sortByString = sortByCombo.getValue();
        int sortBy = ExperimentSorter.SORT_BY_RANGE;
        if ("Speed".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_SPEED;
        } else if ("Angle".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_ANGLE;
        } else if ("Mass".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_MASS;
        } else if ("Gravity".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_GRAVITY;
        } else if ("Air Resistance".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_AIR_RESISTANCE;
        } else if ("Theoretical Range".equals(sortByString)) {
            sortBy = ExperimentSorter.SORT_BY_THEORETICAL_RANGE;
        }

        // Binary search requires sorted data: sort by the chosen field first
        ArrayList<ExperimentRecord> listToSort = new ArrayList<ExperimentRecord>();
        for (int i = 0; i < savedExperiments.size(); i++) {
            listToSort.add(savedExperiments.get(i));
        }

        List<ExperimentRecord> sortedList = ExperimentSorter.mergeSort(listToSort, sortBy);

        // Update the display to show sorted list
        savedExperiments.clear();
        experimentDisplayList.clear();
        for (int i = 0; i < sortedList.size(); i++) {
            ExperimentRecord record = sortedList.get(i);
            savedExperiments.add(record);
            experimentDisplayList.add(formatExperiment(record));
        }

        // Perform binary search to find closest match
        int foundIndex = ExperimentSorter.binarySearchClosestBy(sortedList, targetValue, sortBy);

        if (foundIndex >= 0 && foundIndex < savedExperiments.size()) {
            // Select the found item in the list view
            experimentListView.getSelectionModel().select(foundIndex);
            experimentListView.scrollTo(foundIndex);

            ExperimentRecord found = savedExperiments.get(foundIndex);
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Search Result");
            alert.setHeaderText("Found closest match using Binary Search");
            alert.setContentText(String.format(
                    "Search by: %s\n" +
                            "Target: %.2f\n" +
                            "Found experiment (closest).\n" +
                            "(List sorted by %s for binary search)",
                    sortByString, targetValue, sortByString));
            alert.showAndWait();
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Search Result");
            alert.setHeaderText(null);
            alert.setContentText("No matching experiment found.");
            alert.showAndWait();
        }
    }
}
