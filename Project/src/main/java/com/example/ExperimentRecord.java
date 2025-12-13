package com.example;

public class ExperimentRecord {
    private double speed;
    private double angle;
    private double mass;
    private double landedRange;

    public ExperimentRecord(double speed, double angle, double mass, double landedRange) {
        this.speed = speed;
        this.angle = angle;
        this.mass = mass;
        this.landedRange = landedRange;
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

}
