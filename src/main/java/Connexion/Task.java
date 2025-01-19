package Connexion;

import helpers.Point2D;

import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Task {
    private Timestamp pickup_time, dropoff_time;
    private Point2D dropoff_point, pickup_point;

    public Task(Timestamp pickup_time, Timestamp dropoff_time , Point2D dropoff_point, Point2D pickup_point) {
        this.pickup_time = pickup_time;
        this.dropoff_time = dropoff_time;
        this.dropoff_point = dropoff_point;
        this.pickup_point = pickup_point;
    }

    public Timestamp getPickupTime() {
        return pickup_time;
    }

    public Timestamp getDropoffTime() {
        return dropoff_time;
    }

    public Point2D getDropoffPoint() {
        return dropoff_point;
    }

    public void setDropoffPoint(Point2D dropoff_point) {
        this.dropoff_point = dropoff_point;
    }

    public Point2D getPickupPoint() {
        return pickup_point;
    }

    // Convert a Task to a string
    @Override
    public String toString() {
        return
                "pickup_time=" + pickup_time +
                ", dropoff_time=" + dropoff_time +
                ", dropoff_point=" + dropoff_point +
                ", pickup_point=" + pickup_point;
    }
}