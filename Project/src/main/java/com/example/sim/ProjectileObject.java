package com.example.sim;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Vector2;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class ProjectileObject extends SimObject {
    private double startXMeters;
    private double startYMeters;

    public ProjectileObject(String name, double startXMeters, double startYMeters) {
        super(name);
        this.startXMeters = startXMeters;
        this.startYMeters = startYMeters;
    }

    public abstract double getRadiusMeters();

    protected abstract double getFriction();

    protected abstract double getRestitution();

    protected abstract Color getFillColor();

    @Override
    protected final Body createBody() {
        Body projectile = new Body();

        Circle circle = new Circle(getRadiusMeters());
        BodyFixture fixture = new BodyFixture(circle);
        fixture.setFriction(getFriction());
        fixture.setRestitution(getRestitution());
        fixture.setDensity(1.0);

        projectile.addFixture(fixture);

        Mass mass = circle.createMass(fixture.getDensity());
        projectile.setMass(mass);

        projectile.translate(startXMeters, startYMeters);
        return projectile;
    }

    public final void reset() {
        body.clearForce();
        body.clearTorque();
        body.setLinearVelocity(0, 0);
        body.setAngularVelocity(0);
        body.getTransform().setTranslation(startXMeters, startYMeters);
        body.getTransform().setRotation(0);
        body.setAtRest(false);
    }

    public final void launch(double speedMetersPerSecond, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double vx = speedMetersPerSecond * Math.cos(angleRadians);
        double vy = speedMetersPerSecond * Math.sin(angleRadians);

        body.setAtRest(false);
        body.setLinearVelocity(vx, vy);
    }

    public final void setMassKg(double massKg) {
        if (massKg <= 0) {
            return;
        }

        Circle circle = (Circle) body.getFixture(0).getShape();
        double area = Math.PI * circle.getRadius() * circle.getRadius();
        double density = massKg / area;

        BodyFixture fixture = body.getFixture(0);
        fixture.setDensity(density);

        Mass mass = circle.createMass(density);
        body.setMass(mass);
        body.setAtRest(false);
    }

    public final Vector2 getPosition() {
        return body.getTransform().getTranslation();
    }

    public final Vector2 getVelocity() {
        return body.getLinearVelocity();
    }

    @Override
    public final void render(GraphicsContext gc, Canvas canvas, double pixelsPerMeter) {
        Vector2 position = body.getTransform().getTranslation();
        Circle circle = (Circle) body.getFixture(0).getShape();
        double radius = circle.getRadius();

        double screenX = (canvas.getWidth() / 2) + (position.x * pixelsPerMeter);
        double screenY = (canvas.getHeight() / 2) - (position.y * pixelsPerMeter);
        double screenRadius = radius * pixelsPerMeter;

        gc.setFill(getFillColor());
        gc.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
    }

}
