package com.example.sim;

import javafx.scene.paint.Color;

public class BallProjectile extends ProjectileObject {
    public BallProjectile(double startXMeters, double startYMeters) {
        super("Ball", startXMeters, startYMeters);
    }

    @Override
    public double getRadiusMeters() {
        return 0.5;
    }

    @Override
    protected double getFriction() {
        return 0.2;
    }

    @Override
    protected double getRestitution() {
        return 0.6;
    }

    @Override
    protected Color getFillColor() {
        return Color.RED;
    }
}
