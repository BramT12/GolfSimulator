package com.example.golfgame.utils;

import com.badlogic.gdx.math.Vector2;
import com.example.golfgame.screens.GolfGameScreen;

/**
 * Represents the state of a golf ball, including its position and velocity components.
 * This class provides methods to set and retrieve the ball's position (x, y) and velocity (vx, vy)
 * in a two-dimensional space.
 */
public class BallState {
    private double x = 0;
    private double y = 0;
    private double vx = 0;
    private double vy = 0;

    /**
     * Constructs a new BallState with specified initial position and velocity.
     *
     * @param x  the initial x-coordinate of the ball.
     * @param y  the initial y-coordinate of the ball.
     * @param vx the initial velocity of the ball along the x-axis.
     * @param vy the initial velocity of the ball along the y-axis.
     */
    public BallState(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    /**
     * Sets all components of the ball's state.
     *
     * @param x  the new x-coordinate of the ball.
     * @param y  the new y-coordinate of the ball.
     * @param vx the new velocity of the ball along the x-axis.
     * @param vy the new velocity of the ball along the y-axis.
     */
    public void setAllComponents(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    /**
     * Sets the x-coordinate of the ball.
     *
     * @param x the new x-coordinate of the ball.
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate of the ball.
     *
     * @param y the new y-coordinate of the ball.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Sets the velocity of the ball along the x-axis.
     *
     * @param vx the new velocity along the x-axis.
     */
    public void setVx(double vx) {
        this.vx = vx;
    }

    /**
     * Sets the velocity of the ball along the y-axis.
     *
     * @param vy the new velocity along the y-axis.
     */
    public void setVy(double vy) {
        this.vy = vy;
    }

    /**
     * Sets all components of the ball's state.
     *
     * @param x  the new x-coordinate of the ball.
     * @param y  the new y-coordinate of the ball.
     * @param vx the new velocity of the ball along the x-axis.
     * @param vy the new velocity of the ball along the y-axis.
     */
    public void set(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    /**
     * Returns the x-coordinate of the ball.
     *
     * @return the current x-coordinate of the ball.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of the ball.
     *
     * @return the current y-coordinate of the ball.
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the velocity of the ball along the x-axis.
     *
     * @return the current velocity along the x-axis.
     */
    public double getVx() {
        return vx;
    }

    /**
     * Returns the velocity of the ball along the y-axis.
     *
     * @return the current velocity along the y-axis.
     */
    public double getVy() {
        return vy;
    }

    /**
     * Checks if the state of this ball is approximately equal to another state, within a given tolerance.
     *
     * @param other   the BallState to compare with.
     * @param epsilon the tolerance for the comparison of each double value.
     * @return true if the states are considered equal within the given tolerance, otherwise false.
     */
    public boolean epsilonEquals(BallState other, double epsilon) {
        return Math.abs(this.x - other.x) <= epsilon &&
               Math.abs(this.y - other.y) <= epsilon &&
               Math.abs(this.vx - other.vx) <= epsilon &&
               Math.abs(this.vy - other.vy) <= epsilon;
    }

    /**
     * Checks if the position of this ball is approximately equal to another position, within a given tolerance.
     *
     * @param other   the BallState to compare with.
     * @param epsilon the tolerance for the comparison of each double value.
     * @return true if the positions are considered equal within the given tolerance, otherwise false.
     */
    public boolean epsilonPositionEquals(BallState other, double epsilon) {
        return Math.abs(this.x - other.x) <= epsilon &&
               Math.abs(this.y - other.y) <= epsilon;
    }

    /**
     * Creates and returns a copy of this BallState.
     *
     * @return a new BallState object with the same position and velocity as this one.
     */
    public BallState copy() {
        return new BallState(x, y, vx, vy);
    }

    /**
     * Computes the dot product of the position vectors of this BallState and another.
     *
     * @param other the BallState to compute the dot product with.
     * @return the computed dot product.
     */
    public float positionDot(BallState other) {
        float posX = (float)(x * other.getX());
        float posY = (float)(y * other.getY());
        return posX + posY;
    }

    /**
     * Computes the magnitude of the position vector of this BallState.
     *
     * @return the computed magnitude.
     */
    public float positionMag() {
        return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    /**
     * Computes the magnitude of the velocity vector of this BallState.
     *
     * @return the computed magnitude.
     */
    public float velocityMag() {
        return (float) Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2));
    }

    /**
     * Normalizes the position vector of this BallState.
     */
    public void positionNor() {
        Vector2 pos = new Vector2((float) x, (float) y).nor();
        x = pos.x;
        y = pos.y;
    }

    /**
     * Calculates the Euclidean distance to another BallState position.
     *
     * @param other the other BallState to calculate the distance to.
     * @return the distance to the other BallState.
     */
    public double distanceTo(BallState other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }

    /**
     * Checks if the ball is in water using the game screen.
     *
     * @param gameScreen the game screen to check the water status.
     * @return true if the ball is in water, false otherwise.
     */
    public boolean isInWater(GolfGameScreen gameScreen) {
        return gameScreen.isBallInWater(this);
    }

    /**
     * Checks if the ball is out of bounds using the game screen.
     *
     * @param gameScreen the game screen to check the out-of-bounds status.
     * @return true if the ball is out of bounds, false otherwise.
     */
    public boolean isOutOfBounds(GolfGameScreen gameScreen) {
        return gameScreen.isBallOutOfBounds(this);
    }

    /**
     * Checks if the ball is in the goal.
     *
     * @param goalState the BallState representing the goal position.
     * @return true if the ball is in the goal, false otherwise.
     */
    public boolean isInGoal(BallState goalState) {
        return epsilonPositionEquals(goalState, GolfGameScreen.getGoalTolerance());
    }

    /**
     * Creates and returns a deep copy of this BallState.
     *
     * @return a new BallState object with the same position and velocity as this one.
     */
    public BallState deepCopy() {
        return new BallState(this.x, this.y, this.vx, this.vy);
    }

    @Override
    public String toString() {
        return "BallState{" +
               "x=" + x +
               ", y=" + y +
               ", vx=" + vx +
               ", vy=" + vy +
               '}';
    }
}