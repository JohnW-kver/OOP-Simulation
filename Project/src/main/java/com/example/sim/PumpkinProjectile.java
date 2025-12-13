package com.example.sim;

import javafx.scene.paint.Color;

public class PumpkinProjectile extends ProjectileObject {

    public PumpkinProjectile(double startXMeters, double startYMeters) {
        super("Pumpkin", startXMeters, startYMeters);
    }

    @Override
    public double getRadiusMeters() {
        return 0.6;
    }

    @Override
    protected double getFriction() {
        return 0.6;
    }

    @Override
    protected double getRestitution() {
        return 0.2;
    }

    @Override
    protected Color getFillColor() {
        return Color.ORANGE;
    }

}
