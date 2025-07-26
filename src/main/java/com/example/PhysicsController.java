package com.example;

import java.net.URL;
import java.util.ResourceBundle;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class PhysicsController implements Initializable {

    @FXML
    private Canvas canvas;

    private World world;

    private Body ball;

    private Body ground;

    private void createBall(double radius, double density, double friction, double restitution, double x, double y,
            World world) {
        ball = new Body();

        Circle circle = new Circle(radius);

        BodyFixture fixture = new BodyFixture(circle);
        fixture.setDensity(density);
        fixture.setFriction(friction);
        fixture.setRestitution(restitution);

        ball.addFixture(fixture);

        ball.updateMass();

        ball.translate(x, y);

        world.addBody(ball);

        System.out.println("Ball created: " + ball.getTransform().getTranslation());
    }

    private void createRectangle(double width, double height, double density, double friction, double restitution,
            double x, double y, World world) {
        ground = new Body();

        Rectangle rectangle = new Rectangle(20, 0.3);

        BodyFixture fixture2 = new BodyFixture(rectangle);
        fixture2.setDensity(1);
        fixture2.setFriction(0.2);
        fixture2.setRestitution(0.5);

        ground.addFixture(fixture2);

        ground.updateMass();

        ground.translate(0, -5);

        world.addBody(ground);

        System.out.println("Ground created" + ground.getTransform().getTranslation());
    }

    @Override
    public void initialize(URL url, ResourceBundle resource) {
        System.out.println("PhysicsController initialized");

        // Step 1. create world
        world = new World();

        world.setGravity(new Vector2(0, -9.8));

        // Step 2. create a ball
        createBall(1, 1, 0.2, 0.5, 0, 0, world);

        createRectangle(20, 0.3, 1, 0.2, 0.5, 0, -5, world);

        // Step 3. render
        render();
    }

    /**
     * 
     * Renders the physics world to the canvas
     * 
     * This method converts world coordinates to screen coordinates and draws all
     * objects
     * 
     */
    private void render() {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear the canvas with a light blue background (like sky)
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw the ball
        drawBall(gc);
        System.out.println("Rendered frame - Ball position: " + ball.getTransform().getTranslation());

        // Draw ground
        drawGround(gc);
        System.out.println("Rendered frame - ground position" + ground.getTransform().getTranslation());
    }

    /**
     * 
     * Draws the ball on the canvas
     * 
     * Converts from world coordinates (meters) to screen coordinates (pixels)
     * 
     */
    private void drawBall(GraphicsContext gc) {

        // Get ball position in world coordinates

        Vector2 position = ball.getTransform().getTranslation();

        // Get ball radius from the circle shape
        Circle circle = (Circle) ball.getFixture(0).getShape();
        double radius = circle.getRadius();

        // Convert world coordinates to screen coordinates
        // World coordinate system: (0,0) is center, Y+ is up
        // Screen coordinate system: (0,0) is top-left, Y+ is down
        double screenX = (canvas.getWidth() / 2) + (position.x * 50); // 50 pixels per meter
        double screenY = (canvas.getHeight() / 2) - (position.y * 50); // Flip Y axis
        double screenRadius = radius * 50; // Convert radius to pixels

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

    private void drawGround(GraphicsContext gc) {
        Vector2 position = ground.getTransform().getTranslation();

        Rectangle rectangle = (Rectangle) ground.getFixture(0).getShape();
        double width = rectangle.getWidth();
        double height = rectangle.getHeight();

        double screenX = (canvas.getWidth() / 2) + (position.x * 50);
        double screenY = (canvas.getHeight() / 2) - (position.y * 50);

        double screenWidth = width * 50;
        double screenHeight = height * 50;

        gc.setFill(Color.GRAY);
        gc.fillRect(screenX - screenWidth / 2, screenY - screenHeight / 2, screenWidth, screenHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(screenX - screenWidth / 2, screenY - screenHeight / 2, screenWidth, screenHeight);
    }

}
