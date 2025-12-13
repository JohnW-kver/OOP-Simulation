package com.example.sim;

import org.dyn4j.dynamics.Body;
import org.dyn4j.world.World;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public abstract class SimObject {
    private String name;
    protected Body body;

    public SimObject(String name) {
        this.name = name;
    }

    public Body getBody() {
        return body;
    }

    public String getName() {
        return name;
    }

    public void addToWorld(World world) {
        if (body == null) {
            body = createBody();
        }
        world.addBody(body);
    }

    public void removeFromWorld(World world) {
        if (body != null) {
            world.removeBody(body);
        }
    }

    protected abstract Body createBody();

    public abstract void render(GraphicsContext gc, Canvas canvas, double pixelsPerMeter);

}
