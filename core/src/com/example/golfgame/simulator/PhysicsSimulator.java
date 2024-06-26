package com.example.golfgame.simulator;

import com.badlogic.gdx.math.Vector2;
import com.example.golfgame.bot.agents.PPOAgent;
import com.example.golfgame.utils.*;
import com.example.golfgame.utils.gameUtils.TerrainManager;
import com.example.golfgame.utils.ppoUtils.Action;
import com.example.golfgame.utils.ppoUtils.Batch;
import com.example.golfgame.utils.ppoUtils.State;
import com.example.golfgame.utils.ppoUtils.Transition;
import com.example.golfgame.physics.PhysicsEngine;
import com.example.golfgame.physics.ODE.ODE;
import com.example.golfgame.physics.ODE.RungeKutta;
import com.example.golfgame.screens.GolfGameScreen;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.*;

public class PhysicsSimulator {
    private PhysicsEngine engine;
    private BallState ball;
    private BallState goal;
    private static Random random = new Random(2024);
    private PPOAgent agent;
    private boolean inWater = false;
    private TerrainManager terrainManager;
    private List<Batch> data = new ArrayList<>();
    private List<Function> functions = new ArrayList<>();

    private static final double GOAL_RADIUS = 1.5; // Radius for goal reward
    private static final double PENALTY_WATER = -3; // Penalty for hitting water
    private static final double PENALTY_SAND = -1; // Penalty for being on sand
    private static final double REWARD_GOAL = 5; // Reward for reaching the goal

    private static final float engineStepSize = 0.001f;

    /**
     * Constructs a PhysicsSimulator with specified height function and agent.
     *
     * @param heightFunction the function defining the terrain height.
     * @param agent the PPOAgent used for the simulation.
     */
    public PhysicsSimulator(String heightFunction, PPOAgent agent) {
        addFunction(heightFunction);
        Function fheightFunction = new Function(heightFunction, "x","y");
        this.engine = new PhysicsEngine(new RungeKutta(), fheightFunction);
        this.ball = new BallState(0, 0, 0, 0);
        this.terrainManager = new TerrainManager(fheightFunction);
        this.agent = agent;
        this.goal = new BallState(-7, 7, 0, 0);
    }
    
    /**
     * Constructs a PhysicsSimulator with specified height function and goal state.
     *
     * @param heightFunction the function defining the terrain height.
     * @param goal the target goal state.
     */
    public PhysicsSimulator(Function heightFunction, BallState goal) {
        this.engine = new PhysicsEngine(new RungeKutta(), heightFunction);
        this.ball = new BallState(0, 0, 0, 0);
        this.terrainManager = new TerrainManager(heightFunction);
        this.goal = goal;
    }

    /**
     * Constructs a PhysicsSimulator with specified height function, goal state, and solver.
     *
     * @param heightFunction the function defining the terrain height.
     * @param goal the target goal state.
     * @param solver the ODE solver used for the simulation.
     */
    public PhysicsSimulator(Function heightFunction, BallState goal, ODE solver){
        this.engine = new PhysicsEngine(solver, heightFunction);
        this.ball = new BallState(0, 0, 0.001, 0.001);
        this.terrainManager = new TerrainManager(heightFunction);
        this.goal = goal;
    }

    /**
     * Changes the height function used in the simulation.
     *
     * @param heightFunction the new function defining the terrain height.
     */
    public void changeHeightFunction(Function heightFunction){
        this.engine = new PhysicsEngine(new RungeKutta(), heightFunction);
        this.terrainManager = new TerrainManager(heightFunction);
    }

    /**
     * Performs a hit simulation.
     *
     * @param velocityMagnitude the magnitude of the velocity
     * @param angle the angle of the hit
     * @return the new ball state
     */
    public BallState hit(float velocityMagnitude, float angle) {
        inWater = false;
        BallState ballCopy = ball.deepCopy();
        System.out.printf("Hitting with force: %.2f and angle: %.2f\n", velocityMagnitude, angle);
        ballCopy.setVx(-velocityMagnitude * Math.cos(angle));
        ballCopy.setVy(-velocityMagnitude * Math.sin(angle));

        Map<String, Double> functionVals = new HashMap<>();
        BallState lastBallState = ballCopy.deepCopy(); // Use ballCopy directly

        while (true) {
            functionVals.put("x", ballCopy.getX());
            functionVals.put("y", ballCopy.getY());

            // Check if the ball is in water
            if (terrainManager.isWater((float) ballCopy.getX(), (float) ballCopy.getY())) {
                System.out.println("Ball in water!");
                inWater = true;
                ballCopy.setX(lastBallState.getX());
                ballCopy.setY(lastBallState.getY());
                return ballCopy;
            }

            // Check if the ball has reached the goal
            if (GolfGameScreen.validSimulatorGoal(ballCopy, goal)) {
                System.out.println("Goal reached in simulator!");
                return ballCopy;
            }

            // Update the last ball state before updating the current ball state
            lastBallState.set(ballCopy.getX(), ballCopy.getY(), ballCopy.getVx(), ballCopy.getVy());

            // Update the ball state
            engine.update(ballCopy, engineStepSize);

            // Check if the ball is at rest
            if (engine.isAtRest(ballCopy)) {
                break;
            }
        }

        // Check if the ball is on sand
        if (terrainManager.isBallOnSand((float) ballCopy.getX(), (float) ballCopy.getY())) {
            System.out.println("Ball on sand!");
        }

        System.out.printf("New ball position: (%.2f, %.2f)\n", ballCopy.getX(), ballCopy.getY());
        return ballCopy;
    }

    /**
     * Performs a hit simulation and returns the path.
     *
     * @param velocityMagnitude the magnitude of the velocity
     * @param angle the angle of the hit
     * @return a Pair containing the final BallState and the path of the ball as a list of Vector2 points
     */
    public Pair<BallState, List<Vector2>> hitWithPath(float velocityMagnitude, float angle) {
        inWater = false;
        BallState lastPosition = ball.deepCopy();
        BallState ballCopy = ball.deepCopy();
        System.out.printf("Hitting with force: %.2f and angle: %.2f\n", velocityMagnitude, angle);
        ballCopy.setVx(-velocityMagnitude * Math.cos(angle));
        ballCopy.setVy(-velocityMagnitude * Math.sin(angle));
        List<Vector2> path = new ArrayList<>();
        path.add(new Vector2((float)ballCopy.getX(), (float)ballCopy.getY()));

        BallState lastBallState = null;
        do {
            if (terrainManager.isWater((float) ballCopy.getX(), (float) ballCopy.getY())) { // Water
                System.out.println("Ball in water!");
                inWater = true;
                ballCopy.setX(lastPosition.getX());
                ballCopy.setY(lastPosition.getY());
                return new Pair<>(ballCopy, path);
            }
            lastBallState = new BallState(ballCopy.getX(), ballCopy.getY(), ballCopy.getVx(), ballCopy.getVy());
            engine.update(ballCopy, engineStepSize);
            path.add(new Vector2((float)ballCopy.getX(), (float)ballCopy.getY()));
        } while (!ballCopy.epsilonEquals(lastBallState, 0));

        if (terrainManager.isBallOnSand((float) ballCopy.getX(), (float) ballCopy.getY())) { // Sand
            System.out.println("Ball on sand!");
        }

        System.out.printf("New ball position: (%.2f, %.2f)\n", ballCopy.getX(), ballCopy.getY());
        return new Pair<>(ballCopy, path);
    }

    /**
     * Performs a single hit simulation at a specified position.
     *
     * @param velocityMagnitude the magnitude of the velocity
     * @param angle the angle of the hit
     * @param ballPosition the position of the ball
     * @return the new ball state
     */
    public BallState singleHit(float velocityMagnitude, float angle, BallState ballPosition){
        resetBallPosition(ballPosition);
        return hit(velocityMagnitude, angle);
    }

    /**
     * Computes the reward based on the ball state.
     *
     * @param currentBall the current ball state
     * @param lastPosition the last position of the ball
     * @param win whether the goal is reached
     * @param isBallInWater whether the ball is in water
     * @return the computed reward
     */
    public double getReward(BallState currentBall, BallState lastPosition, boolean win, boolean isBallInWater) {
        double distanceToGoal = currentBall.distanceTo(goal);
        double lastDistanceToGoal = lastPosition.distanceTo(goal);

        // Reward calculation
        double reward = lastDistanceToGoal - distanceToGoal;

        if (distanceToGoal < GOAL_RADIUS) {
            System.out.println("");
            System.out.println("Goal Reached");
            System.out.println("");
            return REWARD_GOAL;
        }
        if (isBallInWater) {
            return reward + PENALTY_WATER;
        }
        if (terrainManager.isBallOnSand((float) currentBall.getX(), (float) currentBall.getY())) {
            return reward + PENALTY_SAND;
        }
        if (reward < 0) {
            double penaltyFactor = Math.exp(Math.abs(reward) / 10.0); // Exponential function
            reward -= penaltyFactor * 10; // Increase the penalty
        }
        return reward;
    }

    /**
     * Performs multiple hit simulations.
     *
     * @param velocityMagnitudes array of velocity magnitudes
     * @param angles array of angles
     * @return array of resulting ball states
     */
    public BallState[] hit(float[] velocityMagnitudes, float[] angles) {
        BallState[] res = new BallState[velocityMagnitudes.length];
        for (int i = 0; i < velocityMagnitudes.length; i++) {
            res[i] = hit(velocityMagnitudes[i], angles[i]);
            resetBallPosition();
        }
        return res;
    }

    /**
     * Resets the ball position to the initial state.
     */
    private void resetBallPosition() {
        ball.setX(0);
        ball.setY(0);
    }

    /**
     * Resets the ball position to a specified state.
     *
     * @param ballPosition the position to reset the ball to
     */
    private void resetBallPosition(BallState ballPosition){
        ball.setX(ballPosition.getX());
        ball.setY(ballPosition.getY());
    }
    
    /**
     * Performs random hit simulations within a certain radius of the goal.
     *
     * @param n number of simulations
     * @param goal target goal state
     * @param radius radius around the goal
     * @return array of resulting ball states
     */
    public BallState[] randomHits(int n, BallState goal, float radius) {
        BallState[] res = new BallState[n];
        for (int i = 0; i < n; i++) {
            float ballX = random.nextFloat() * (2 * radius) - radius;
            float ballY = random.nextBoolean() ? (float) Math.sqrt(radius * radius - ballX * ballX) : -(float) Math.sqrt(radius * radius - ballX * ballX);
            ballX += goal.getX();
            ballY += goal.getY();
            ball.setX(ballX);
            ball.setY(ballY);
            float velocityMagnitude = random.nextFloat() * (5 - 1) + 1;
            float angle = random.nextFloat() * (2 * (float) Math.PI);
            res[i] = hit(velocityMagnitude, angle);
        }
        return res;
    }

    /**
     * Sets the ball position.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void setPosition(float x, float y) {
        ball.setX(x);
        ball.setY(y);
    }

    /**
     * Returns the current state of the terrain.
     *
     * @return a flattened array representing the normalized height map with marked ball, goal, and sand positions
     */
    public double[] getState() {
        return MatrixUtils.flattenArray(terrainManager.getNormalizedMarkedHeightMap((float) ball.getX(), (float) ball.getY(), (float) goal.getX(), (float) goal.getY()));
    }

    /**
     * Saves the height map as an image.
     */
    @SuppressWarnings("static-access")
    public void image(){
        terrainManager.saveHeightMapAsImage(terrainManager.getNormalizedMarkedHeightMap((float) ball.getX(), (float) ball.getY(), (float) goal.getX(), (float) goal.getY()), "height_map", "png");
    }

    /**
     * Adds a function to the list of functions.
     *
     * @param function the function to add
     */
    public void addFunction(String function){
        functions.add(new Function(function, "x","y"));
    }

    /**
     * Runs a simulation for a specified number of episodes.
     *
     * @param episodes the number of episodes to run
     * @param radius the radius around the goal
     * @param steps the number of steps in each episode
     */
    public void runSimulation(int episodes, float radius, int steps) {
        for(Function function : functions){
            changeHeightFunction(function);
            for (int episode = 0; episode < episodes; episode++) {
                data.add(runSingleEpisode(radius,(int) Math.round(steps*0.2),true));
                data.add(runSingleEpisode(radius,(int) Math.round(steps*0.8),false));
                System.out.println("Episode: " + episode);
            }
        }
        agent.trainOnData(data);
    }

    /**
     * Runs a parallel simulation for a specified number of episodes.
     *
     * @param episodes the number of episodes to run
     * @param radius the radius around the goal
     * @param steps the number of steps in each episode
     */
    public void runSimulationParallel(int episodes, float radius, int steps) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Batch>> futures = new ArrayList<>();
        
        for (Function function : functions) {
            changeHeightFunction(function);
            
            for (int episode = 0; episode < episodes; episode++) {
                final int ep = episode; // For lambda expression
                Callable<Batch> task = () -> {
                    if (ep % 2 == 0) {
                        return runSingleEpisode(radius, (int) Math.round(steps * 0.2), true);
                    } else {
                        return runSingleEpisode(radius, (int) Math.round(steps * 0.8), false);
                    }
                };
                futures.add(executor.submit(task));
            }
        }
        
        for (Future<Batch> future : futures) {
            try {
                data.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        agent.trainOnData(data);
    }

    /**
     * Runs a single episode of the simulation.
     *
     * @param radius the radius around the goal
     * @param steps the number of steps in the episode
     * @param randomAction whether to use random actions
     * @return a Batch containing the transitions for the episode
     */
    private Batch runSingleEpisode(float radius, int steps, boolean randomAction) {
        List<Transition> batchTransitions = new ArrayList<>();
        resetBallPosition();
        float ballX = random.nextFloat() * (2 * radius) - radius;
        float ballY = random.nextBoolean() ? (float) Math.sqrt(radius * radius - ballX * ballX) : -(float) Math.sqrt(radius * radius - ballX * ballX);
        ballX += goal.getX();
        ballY += goal.getY();
        ball.setX(ballX);
        ball.setY(ballY);
        double totalReward = 0;
        BallState lastPosition = new BallState(ball.getX(), ball.getY(), ball.getVx(), ball.getVy());

        for (int step = 0; step < steps; step++) {
            double[] stateArray = getState();
            State state = new State(stateArray);
            Action action;
            if(randomAction){
                action = agent.selectRandomAction();
            }else{
                action = agent.selectAction(state);
            }
            BallState newBallState = hit((float) action.getForce(), (float) action.getAngle());
            boolean win = newBallState.distanceTo(goal) < GOAL_RADIUS;
            double reward = getReward(newBallState, lastPosition, win, inWater);
            totalReward += reward;
            double[] newStateArray = getState();
            State newState = new State(newStateArray);
            Transition transition = new Transition(state, action, reward, newState);
            batchTransitions.add(transition);
            lastPosition = new BallState(newBallState.getX(), newBallState.getY(), newBallState.getVx(), newBallState.getVy());
            if (win) {
                break;
            }
        }

        System.out.println("Total Reward: "+ totalReward);
        return new Batch(batchTransitions);
    }
}