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
import javafx.scene.paint.Color;

public class PhysicsController implements Initializable {

    // === PHYSICS CONSTANTS (easy to modify for experimentation) ===

    /** Gravity strength in m/s² (negative because Y+ is up in Dyn4j) */

    private static final double GRAVITY = -9.8;

    /** Ground width in meters */

    private static final double GROUND_WIDTH = 20.0;

    /** Ground height in meters */

    private static final double GROUND_HEIGHT = 1.0;

    /** Ground Y position in meters (negative = below center) */

    private static final double GROUND_Y_POSITION = -3.0;

    /** Pixels per meter for coordinate conversion */

    private static final double PIXELS_PER_METER = 50.0;

    private static final String SAVE_FILE = "physics_experiments.csv";

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
    private Button launchButton;

    @FXML
    private Button resetButton;

    @FXML
    private Label speedLabel;

    @FXML
    private Label labelHeight;

    @FXML
    private Label labelRange;

    @FXML
    private Label labelDroppedRange;

    @FXML
    private Slider speedSlider;

    @FXML
    private Label massLabel;

    @FXML
    private Slider massSlider;

    @FXML
    private ComboBox<String> projectileTypeCombo;

    @FXML
    private Button saveButton;

    @FXML
    private Button loadButton;

    @FXML
    private ListView<String> experimentListView;

    private ObservableList<ExperimentRecord> savedExperiments = FXCollections.observableArrayList();
    private ObservableList<String> experimentDisplayList = FXCollections.observableArrayList();

    private double projectileStartX = -8;
    private double projectileStartY = -2;
    private Double landedRange = null;
    private boolean hasBeenLaunched = false;
    private boolean hasBeenAirborne = false;
    private static final double GROUND_CONTACT_TOLERANCE = 0.1;

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
        world.setGravity(new Vector2(0.0, GRAVITY));

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
                });
        massSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    massLabel.setText(String.format("%.1f kg", newValue.doubleValue()));
                    if (projectileObject != null) {
                        projectileObject.setMassKg(newValue.doubleValue());
                    }
                });
        resetButton.setOnAction(event -> resetProjectile());
        launchButton.setOnAction(event -> launchProjectile());
        saveButton.setOnAction(event -> saveCurrentExperiment());
        loadButton.setOnAction(event -> loadExperimentsFromFile());
        experimentListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex == null || newIndex.intValue() < 0 || newIndex.intValue() >= savedExperiments.size()) {
                return;
            }
            applyExperimentToControls(savedExperiments.get(newIndex.intValue()));
        });

        experimentListView.setItems(experimentDisplayList);
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
        simObjects.add(projectileObject);

        resetProjectile();
    }

    private void applyExperimentToControls(ExperimentRecord record) {
        projectileObject.reset();
        landedRange = null;
        hasBeenLaunched = false;
        hasBeenAirborne = false;
        trajectoryTrace.clear();

        speedSlider.setValue(record.getSpeed());
        massSlider.setValue(record.getMass());
        angleField.setText(String.format("%.1f", record.getAngle()));

        labelDroppedRange.setText(String.format("%.2f m", record.getLandedRange()));
        labelHeight.setText("0.00 m");
        labelRange.setText("0.00 m");
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
                if (parts.length != 4) {
                    continue;
                }

                try {
                    double speed = Double.parseDouble(parts[0]);
                    double angle = Double.parseDouble(parts[1]);
                    double mass = Double.parseDouble(parts[2]);
                    double range = Double.parseDouble(parts[3]);

                    ExperimentRecord record = new ExperimentRecord(speed, angle, mass, range);
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
            ExperimentRecord record = new ExperimentRecord(speed, angle, mass, landedRange);
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
        return String.format("Speed: %.1f m/s, Angle: %.1f°, Mass: %.1f kg, Range: %.2f m",
                record.getSpeed(), record.getAngle(), record.getMass(), record.getLandedRange());
    }

    public void saveExperimentToCsv(ExperimentRecord record) {
        String line = String.format("%.2f,%.2f,%.2f,%.2f\n", record.getSpeed(), record.getAngle(), record.getMass(),
                record.getLandedRange());

        try {
            Path savePath = Path.of(SAVE_FILE);
            if (!Files.exists(savePath)) {
                Files.writeString(savePath, "speed,angle,mass,range\n");
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
        labelDroppedRange.setText("-- m");
        labelHeight.setText("0.00 m");
        labelRange.setText("0.00 m");
    }

    private void launchProjectile() {
        double speed = speedSlider.getValue();
        String angleString = angleField.getText().trim();
        try {
            double angle = Double.parseDouble(angleString);

            landedRange = null;
            hasBeenLaunched = true;
            hasBeenAirborne = false;
            labelDroppedRange.setText("-- m");

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

        labelHeight.setText(String.format(("%.2f m"), height));
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
}
