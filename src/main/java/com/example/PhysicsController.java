package com.example;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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

    // List to store all ball bodies
    private List<Body> balls;

    // Ground body to prevent infinite falling
    private Body ground;

    // Animation timer for continuous updates
    private AnimationTimer gameLoop;

    // Timing variables for physics updates
    private long lastUpdateTime = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("PhysicsController initialized!");
        System.out.println("Canvas size: " + canvas.getWidth() + "x" + canvas.getHeight());

        // Initialize the balls list
        balls = new ArrayList<>();

        // Step 1: Create the physics world
        setupPhysicsWorld();

        // Step 2: Create a simple falling ball
        createFallingBall();

        // Step 3: Create ground so ball doesn't fall forever
        createGround();

        // Step 4: Set up mouse interaction
        setupMouseInteraction();

        // Step 5: Draw the initial state
        render();

        // Step 6: Start the animation loop
        startGameLoop();

        System.out.println("Physics world created with " + world.getBodyCount() + " bodies");
        System.out.println("Animation loop started!");
        System.out.println("Click on the canvas to create new balls!");
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

    /**
     * Creates a simple ball that will fall due to gravity at the center
     */
    private void createFallingBall() {
        createBallAt(0.0, 3.0);
    }

    /**
     * Creates a ball at the specified world coordinates
     * 
     * @param worldX X position in world coordinates (meters)
     * @param worldY Y position in world coordinates (meters)
     */
    private void createBallAt(double worldX, double worldY) {
        // Create a new body (this represents our ball)
        Body ball = new Body();

        // Create a circle shape for the ball using our constant
        Circle circle = new Circle(BALL_RADIUS);

        // Create a fixture to attach the shape to the body
        // The fixture defines physical properties like density, friction, etc.
        BodyFixture fixture = new BodyFixture(circle);
        fixture.setDensity(BALL_DENSITY); // Ball density in kg/m²
        fixture.setFriction(BALL_FRICTION); // Friction coefficient
        fixture.setRestitution(BALL_RESTITUTION); // Bounciness factor

        // Add the fixture to the body
        ball.addFixture(fixture);

        // Set the mass based on the fixtures (this is important!)
        // Let's manually create a mass from the circle shape
        Mass mass = circle.createMass(fixture.getDensity());
        ball.setMass(mass);

        // Position the ball at the specified coordinates
        ball.translate(worldX, worldY);

        // Add the ball to the physics world
        world.addBody(ball);

        // Add the ball to our list for rendering
        balls.add(ball);

        System.out.println("Ball created at position: " + ball.getTransform().getTranslation());
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

    /**
     * Sets up mouse interaction to create balls when clicking on the canvas
     */
    private void setupMouseInteraction() {
        canvas.setOnMouseClicked(this::onCanvasClicked);
        System.out.println("Mouse interaction set up - click on canvas to create balls!");
    }

    /**
     * Handles mouse clicks on the canvas to create new balls
     * 
     * @param event The mouse click event
     */
    private void onCanvasClicked(MouseEvent event) {
        // Convert screen coordinates to world coordinates
        double screenX = event.getX();
        double screenY = event.getY();

        // Convert to world coordinates (reverse of the rendering conversion)
        double worldX = (screenX - canvas.getWidth() / 2) / PIXELS_PER_METER;
        double worldY = (canvas.getHeight() / 2 - screenY) / PIXELS_PER_METER;

        // Create a new ball at the clicked position
        createBallAt(worldX, worldY);

        System.out.println("Created ball at world coordinates: (" + worldX + ", " + worldY + ")");
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

        // Optional: Print ball count occasionally for debugging
        // Uncomment the lines below if you want to see the ball count in console
        // if (System.currentTimeMillis() % 1000 < 50) {
        // System.out.println("Total balls: " + balls.size());
        // }
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

        // Draw all balls
        for (Body ball : balls) {
            drawBall(gc, ball);
        }

        // Draw the ground
        drawGround(gc);
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
