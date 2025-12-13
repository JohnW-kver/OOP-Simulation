package com.example.sim;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GroundObject extends SimObject {
    private double heightMeters;
    private double widthMeters;
    private double yMeters;

    public GroundObject(double widthMeters, double heightMeters, double yMeters) {
        super("Ground");
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
        this.yMeters = yMeters;
    }

    @Override
    protected Body createBody() {
        Body ground = new Body();

        Rectangle rectangle = new Rectangle(widthMeters, heightMeters);
        BodyFixture fixture = new BodyFixture(rectangle);
        fixture.setDensity(1.0);
        fixture.setFriction(0.5);
        fixture.setRestitution(0.3);

        ground.addFixture(fixture);
        ground.translate(0.0, yMeters);
        return ground;
    }

    public double getTopY() {
        return yMeters + (heightMeters / 2.0);
    }

    public double getHeightMeters() {
        return heightMeters;
    }

    @Override
    public void render(GraphicsContext gc, Canvas canvas, double pixelsPerMeter) {
        Vector2 position = body.getTransform().getTranslation();
        Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();

        double width = rectangle.getWidth();
        double height = rectangle.getHeight();

        double screenX = (canvas.getWidth() / 2) + (position.x * pixelsPerMeter);
        double screenY = (canvas.getHeight() / 2) - (position.y * pixelsPerMeter);
        double screenWidth = width * pixelsPerMeter;
        double screenHeight = height * pixelsPerMeter;

        gc.setFill(Color.BROWN);
        gc.fillRect(screenX - screenWidth / 2, screenY - screenHeight / 2, screenWidth, screenHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(screenX - screenWidth / 2, screenY - screenHeight / 2, screenWidth, screenHeight);
    }

}
