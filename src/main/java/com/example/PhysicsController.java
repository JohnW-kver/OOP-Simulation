package com.example;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class PhysicsController implements Initializable {

    // === PHYSICS CONSTANTS (easy to modify for experimentation) ===

    /** Gravity strength in m/s² (negative because Y+ is up in Dyn4j) */
    private static final double GRAVITY = -9.8;

    /** Ball radius in meters */
    private static final double BALL_RADIUS = 0.5;

    /** Ball density in kg/m² */
    private static final double BALL_DENSITY = 1.0;

    /** Ball friction coefficient (0 = no friction, 1 = high friction) */
    private static final double BALL_FRICTION = 0.2;

    /** Ball restitution/bounciness (0 = no bounce, 1 = perfect bounce) */
    private static final double BALL_RESTITUTION = 0.6;

    /** Ground width in meters */
    private static final double GROUND_WIDTH = 20.0;

    /** Ground height in meters */
    private static final double GROUND_HEIGHT = 1.0;

    /** Ground Y position in meters (negative = below center) */
    private static final double GROUND_Y_POSITION = -3.0;

    /** Pixels per meter for coordinate conversion */
    private static final double PIXELS_PER_METER = 50.0;

    // === JavaFX COMPONENTS ===

    @FXML
    private Canvas canvas;

    // Physics world - this is where all the physics simulation happens
    private World world;

    // Ground body to prevent infinite falling
    private Body ground;

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
    private Body projectile;

    private double projectileStartX = -8;
    private double projectileStartY = 0;
    private Double landedRange = null;
    private static final double LANDED_SPEED_THRESHOLD = 0.1;
    private static final double LANDED_HEIGHT_TOLERANCE = 0.02;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("PhysicsController initialized!");
        System.out.println("Canvas size: " + canvas.getWidth() + "x" + canvas.getHeight());
        // Step 1: Create the physics world
        setupPhysicsWorld();

        // Step 2: Create a simple falling ball
        createProjectile();

        // Step 3: Create ground so ball doesn't fall forever
        createGround();

        // Step 4: Set up mouse interaction
        setupUIControls();

        // Step 5: Draw the initial state
        render();

        // Step 6: Start the animation loop
        startGameLoop();
    }

    /**
     * Creates and configures the physics world
     */
    private void setupPhysicsWorld() {
        // Create a new physics world with default settings
        world = new World();

        // Set gravity (pointing downward)
        // In Dyn4j, positive Y is up, so gravity should be negative
        world.setGravity(new Vector2(0.0, GRAVITY));

        System.out.println("Physics world created with gravity: " + world.getGravity());
    }

    private void createProjectile() {
        // Create a new body (this represents our ball)
        projectile = new Body();

        // Create a circle shape for the ball using our constant
        Circle circle = new Circle(BALL_RADIUS);

        // Create a fixture to attach the shape to the body
        // The fixture defines physical properties like density, friction, etc.
        BodyFixture fixture = new BodyFixture(circle);
        fixture.setDensity(BALL_DENSITY); // Ball density in kg/m²
        fixture.setFriction(BALL_FRICTION); // Friction coefficient
        fixture.setRestitution(BALL_RESTITUTION); // Bounciness factor

        // Add the fixture to the body
        projectile.addFixture(fixture);

        // Set the mass based on the fixtures (this is important!)
        // Let's manually create a mass from the circle shape
        Mass mass = circle.createMass(fixture.getDensity());
        projectile.setMass(mass);

        // Position the projectile at the specified coordinates
        projectile.translate(projectileStartX, projectileStartY);

        // Add the projectile to the physics world
        world.addBody(projectile);
    }

    /**
     * Creates a static ground body at the bottom of the screen
     */
    private void createGround() {
        // Create a new body for the ground
        ground = new Body();

        // Create a rectangle shape for the ground using our constants
        Rectangle rectangle = new Rectangle(GROUND_WIDTH, GROUND_HEIGHT);

        // Create a fixture for the ground
        BodyFixture fixture = new BodyFixture(rectangle);
        fixture.setDensity(1.0); // Density doesn't matter for static bodies
        fixture.setFriction(0.5); // Some friction for realistic bouncing
        fixture.setRestitution(0.3); // Some bounciness

        // Add the fixture to the ground body
        ground.addFixture(fixture);

        // Don't set mass - bodies are static (infinite mass) by default in Dyn4j

        // Position the ground at the bottom of the screen using our constant
        ground.translate(0.0, GROUND_Y_POSITION);

        // Add the ground to the physics world
        world.addBody(ground);

        System.out.println("Ground created at position: " + ground.getTransform().getTranslation());
    }

    private void setupUIControls() {
        speedSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    speedLabel.setText(String.format("%.1f m/s", newValue.doubleValue()));
                });
        massSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    massLabel.setText(String.format("%.1f kg", newValue.doubleValue()));
                });
        resetButton.setOnAction(event -> resetProjectile());
        launchButton.setOnAction(event -> launchProjectile());
    }

    private void resetProjectile() {
        projectile.clearForce();
        projectile.clearTorque();
        projectile.setLinearVelocity(0, 0);
        projectile.setAngularVelocity(0);

        projectile.getTransform().setTranslation(projectileStartX, projectileStartY);

        projectile.getTransform().setRotation(0);
        projectile.setAtRest(false);
    }

    private void launchProjectile() {
        double speed = speedSlider.getValue();
        String angleString = angleField.getText().trim();
        try {
            double angle = Double.parseDouble(angleString);

            double angleRadians = Math.toRadians(angle);
            double vx = speed * Math.cos(angleRadians);
            double vy = speed * Math.sin(angleRadians);

            projectile.setAtRest(false);
            projectile.setLinearVelocity(vx, vy);

        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Starts the game loop that continuously updates physics and renders the scene
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
     * Updates the physics simulation
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    private void updatePhysics(double deltaTime) {
        // Update the physics world
        // The step method advances the simulation by the given time
        world.update(deltaTime);
    }

    /**
     * Renders the physics world to the canvas
     * This method converts world coordinates to screen coordinates and draws all
     * objects
     */
    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear the canvas with a light blue background (like sky)
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawBall(gc, projectile);

        // Draw the ground
        drawGround(gc);

        Vector2 position = projectile.getTransform().getTranslation();
        double groundTop = GROUND_Y_POSITION + (GROUND_HEIGHT / 2.0);
        double height = Math.max(0.0, position.y - groundTop - BALL_RADIUS);
        double range = Math.max(0.0, position.x - projectileStartX);

        labelHeight.setText(String.format(("%.2f m"), height));
        labelRange.setText(String.format(("%.2f m"), range));

        Vector2 velocity = projectile.getLinearVelocity();
        if (landedRange == null && hasLanded(position, velocity)) {
            landedRange = range;
        }
        if (landedRange != null) {
            labelDroppedRange.setText(String.format("%.2f m", landedRange));
        }
    }

    private boolean hasLanded(Vector2 position, Vector2 velocity) {
        double groundTop = GROUND_Y_POSITION + (GROUND_HEIGHT / 2.0);
        double height = position.y - groundTop - BALL_RADIUS;
        return height <= LANDED_HEIGHT_TOLERANCE && velocity.getMagnitude() <= LANDED_SPEED_THRESHOLD
                && velocity.y <= LANDED_SPEED_THRESHOLD;
    }

    /**
     * Draws a ball on the canvas
     * Converts from world coordinates (meters) to screen coordinates (pixels)
     * 
     * @param gc   Graphics context to draw on
     * @param ball The ball body to draw
     */
    private void drawBall(GraphicsContext gc, Body ball) {
        // Get ball position in world coordinates
        Vector2 position = ball.getTransform().getTranslation();

        // Get ball radius from the circle shape
        Circle circle = (Circle) ball.getFixture(0).getShape();
        double radius = circle.getRadius();

        // Convert world coordinates to screen coordinates
        // World coordinate system: (0,0) is center, Y+ is up
        // Screen coordinate system: (0,0) is top-left, Y+ is down
        double screenX = (canvas.getWidth() / 2) + (position.x * PIXELS_PER_METER);
        double screenY = (canvas.getHeight() / 2) - (position.y * PIXELS_PER_METER);
        double screenRadius = radius * PIXELS_PER_METER;

        // Draw the ball as a red circle
        gc.setFill(Color.RED);
        gc.fillOval(screenX - screenRadius, screenY - screenRadius,
                screenRadius * 2, screenRadius * 2);

        // Draw a black outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(screenX - screenRadius, screenY - screenRadius,
                screenRadius * 2, screenRadius * 2);
    }

    /**
     * Draws the ground on the canvas
     * Converts from world coordinates (meters) to screen coordinates (pixels)
     */
    private void drawGround(GraphicsContext gc) {
        // Get ground position in world coordinates
        Vector2 position = ground.getTransform().getTranslation();

        // Get ground dimensions from the rectangle shape
        Rectangle rectangle = (Rectangle) ground.getFixture(0).getShape();
        double width = rectangle.getWidth();
        double height = rectangle.getHeight();

        // Convert world coordinates to screen coordinates
        double screenX = (canvas.getWidth() / 2) + (position.x * PIXELS_PER_METER);
        double screenY = (canvas.getHeight() / 2) - (position.y * PIXELS_PER_METER);
        double screenWidth = width * PIXELS_PER_METER;
        double screenHeight = height * PIXELS_PER_METER;

        // Draw the ground as a brown rectangle
        gc.setFill(Color.BROWN);
        gc.fillRect(screenX - screenWidth / 2, screenY - screenHeight / 2,
                screenWidth, screenHeight);

        // Draw a black outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(screenX - screenWidth / 2, screenY - screenHeight / 2,
                screenWidth, screenHeight);
    }
}
