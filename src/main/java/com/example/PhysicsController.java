package com.example;

import java.net.URL;
import java.util.ResourceBundle;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
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

    @Override
    public void initialize(URL url, ResourceBundle resource) {
        System.out.println("PhysicsController initialized");

        // Step 1. create world
        world = new World();

        world.setGravity(new Vector2(0, -9.8));

        // Step 2. create a ball
        ball = new Body();

        Circle circle = new Circle(2);

        BodyFixture fixture = new BodyFixture(circle);
        fixture.setDensity(1);
        fixture.setFriction(0.2);
        fixture.setRestitution(0.5);

        ball.addFixture(fixture);

        ball.updateMass();

        ball.translate(0, 3);

        world.addBody(ball);

        System.out.println("Ball created: " + ball.getTransform().getTranslation());

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

}
