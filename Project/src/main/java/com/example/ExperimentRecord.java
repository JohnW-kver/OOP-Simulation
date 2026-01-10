package com.example;

public class ExperimentRecord {
    private double speed;
    private double angle;
    private double mass;
    private double landedRange;
    private double maxHeight;
    private double gravity;
    private double airResistance;

    private static final double DEFAULT_GRAVITY = 9.8;

    public ExperimentRecord(double speed, double angle, double mass, double landedRange) {
        this.speed = speed;
        this.angle = angle;
        this.mass = mass;
        this.landedRange = landedRange;
        this.maxHeight = 0.0;
        this.gravity = DEFAULT_GRAVITY;
        this.airResistance = 0.0;
    }

    public ExperimentRecord(double speed, double angle, double mass, double landedRange, double maxHeight) {
        this.speed = speed;
        this.angle = angle;
        this.mass = mass;
        this.landedRange = landedRange;
        this.maxHeight = maxHeight;
        this.gravity = DEFAULT_GRAVITY;
        this.airResistance = 0.0;
    }

    public ExperimentRecord(double speed, double angle, double mass, double landedRange, double maxHeight,
            double gravity,
            double airResistance) {
        this.speed = speed;
        this.angle = angle;
        this.mass = mass;
        this.landedRange = landedRange;
        this.maxHeight = maxHeight;
        this.gravity = gravity;
        this.airResistance = airResistance;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getLandedRange() {
        return landedRange;
    }

    public void setLandedRange(double landedRange) {
        this.landedRange = landedRange;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        this.gravity = gravity;
    }

    public double getAirResistance() {
        return airResistance;
    }

    public void setAirResistance(double airResistance) {
        this.airResistance = airResistance;
    }
}
