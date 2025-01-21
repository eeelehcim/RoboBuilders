package Connexion;

import ev3dev.sensors.Battery;
import helpers.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static jade.lang.acl.ACLMessage.INFORM;

public class AgentRobot extends Agent {
    static EV3MediumRegulatedMotor leftMotor = new EV3MediumRegulatedMotor(MotorPort.B);
    static EV3MediumRegulatedMotor rightMotor = new EV3MediumRegulatedMotor(MotorPort.A);
    static EV3UltrasonicSensor frontSensor = new EV3UltrasonicSensor(SensorPort.S1);
    static EV3UltrasonicSensor rightSensor = new EV3UltrasonicSensor(SensorPort.S3);
    static EV3UltrasonicSensor leftSensor = new EV3UltrasonicSensor(SensorPort.S4);
    static TagIdMqtt tag;

    // Tasks
    Point2D[] charging_points = new Point2D[2];
    Point2D targetPoint;
    List<Task> tasks = new ArrayList<>();
    Task currentTask;
    long tasksTime;
    boolean isPickup = true; // True if we are heading towards the pickup point
    int RESTART_TASK_LIST_MS = 300000;

    // Battery constants VALUES FOR TESTING
    int TOTAL_BATTERY_LEVEL = 240000;                                    // Battery cannot exceed 240000MS = 4 minutes
    private double simulatedBatteryLevel_ms = TOTAL_BATTERY_LEVEL;      // Initial battery level
    int BATTERY_TO_GO_CHARGING_MS = 60000;                              // 1 minute in ms
    private long notifyTimeBeforeFinish = 10 * 60 * 1000;                         // 10 minutes in ms

    /* REAL VALUES
    int TOTAL_BATTERY_LEVEL = 240000;                       // Battery cannot exceed 240000MS = 4 minutes
    private double simulatedBatteryLevel_ms = 240000;       // Initial battery level
    int BATTERY_TO_GO_CHARGING_MS = 60000;                  // 1 minute in ms
    int TOTAL_TIME_CHARGING = 20*60*100;                    // 20 min
    private long notifyTimeBeforeFinish = 10 * 60 * 1000;   // 10 minutes in ms*/


    //Triaging level constants
    private int ETHYLENE_THRESHOLD = 90;                        // Threshold in ppm


    @Override
    protected void setup() {
        System.out.println("Local name Robot: " + getAID().getLocalName());
        System.out.println("Global name Robot: " + getAID().getName());
        System.out.println(Battery.getInstance().getVoltage());

        try {
            tag = new TagIdMqtt("6841");
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();
                if (message != null) {
                    System.out.println("Agent Robot Received: " + message.getContent());
                }
            }
        });

        addBehaviour(updateLocation);
        addBehaviour(initMessage);
        addBehaviour(ReceiveTasksBehaviour);
        addBehaviour(robotControlBehaviour);
    }

    @Override
    protected void takeDown() {
        leftMotor.stop();
        rightMotor.stop();
        System.out.println("Agent shutting down.");
    }

    // NOTIFICATION TO THE CENTRAL MONITOR
    private void notifyCentralMonitor(String content) {
        ACLMessage message = new ACLMessage(INFORM);
        message.addReceiver(new AID("AgentMain@192.168.0.161:1099/JADE", AID.ISGUID));
        message.setContent(content);
        send(message);
    }

    OneShotBehaviour initMessage = new OneShotBehaviour() {
        @Override
        public void action() {
            notifyCentralMonitor("This is one-shot - I am alive.");
        }
    };

    OneShotBehaviour ReceiveTasksBehaviour = new OneShotBehaviour() {
        @Override
        public void action() {
            notifyCentralMonitor("request-tasks");

            ACLMessage message = blockingReceive(2000); // Wait up to 2 seconds for a response
            if (message == null) {
                System.out.println("No response received from AgentMain.");
            }
            System.out.println(message);
            if (message != null) {
                tasksTime = System.currentTimeMillis();
                String[] taskStrings = message.getContent().split("\n");
                for (String taskString : taskStrings) {
                    if (!taskString.trim().isEmpty()) {
                        Task task = parseTask(taskString);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }
                }
                if (!tasks.isEmpty() && currentTask == null) {
                    currentTask = tasks.remove(0);
                    targetPoint = currentTask.getPickupPoint();
                    isPickup = true;
                }
            } else {
                System.out.println("Error");
            }

        }

        private Task parseTask(String taskString) {
            try {
                if (taskString == null || taskString.trim().isEmpty()) {
                    throw new IllegalArgumentException("Task string is empty or null.");
                }

                String[] parts = taskString.split(",");
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Task string does not contain the expected number of parts. | Input: " + taskString);
                }

                Timestamp pickupTime = Timestamp.valueOf(parts[0].split("=")[1].trim());
                Timestamp dropoffTime = Timestamp.valueOf(parts[1].split("=")[1].trim());
                Point2D dropoffPoint = parsePoint(parts[2].split("=")[1].trim());
                Point2D pickupPoint = parsePoint(parts[3].split("=")[1].trim());

                return new Task(pickupTime, dropoffTime, dropoffPoint, pickupPoint);
            } catch (Exception e) {
                System.out.println("Failed to parse task: " + e.getMessage());
                return null;
            }
        }

        public Point2D parsePoint(String pointString) {
            // Example format: "{x: 13165; y:14609}"
            pointString = pointString.replace("{x:", "").replace("}", "").replace("y:", "").trim();
            String[] coordinates = pointString.split(";");

            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Invalid point format: " + pointString);
            }

            int x = Integer.parseInt(coordinates[0].trim());
            int y = Integer.parseInt(coordinates[1].trim());
            return new Point2D(x, y);
        }
    };

    TickerBehaviour updateLocation = new TickerBehaviour(this, 1000) {
        @Override
        public void onTick() {
            try {
                Point2D loc = tag.getLocation();
                notifyCentralMonitor("location " + loc.x + " " + loc.y + " " + tag.getAngle());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    };

    TickerBehaviour robotControlBehaviour = new TickerBehaviour(this, 100) {
        int frontDistance = (int) getUltrasonicDistance(frontSensor);
        int frontSafeDistance = 20;
        int oldFrontDistance = frontSafeDistance + 20;
        double distanceToTarget = 0;
        boolean turnLeftNext = true;
        boolean obstacleLeft = true;
        int sideSafeDistance = 10;
        boolean charging = false;
        boolean needsCharging = false;
        boolean avoidingObstacle = false;

        @Override
        protected void onTick() {
            drainBattery(41.67); // Drain 0.4% battery per tick --> battery constrain is 4 minutes

            // Check battery level and handle charging
            if (simulatedBatteryLevel_ms < BATTERY_TO_GO_CHARGING_MS && !charging) {   // When the robot has 1 minute of battery left, we have to go charge it. From here comes the 60
                System.out.println("Battery critically low! Going to charge.");
                String tasks_left = tasks.toString();
                notifyCentralMonitor("Battery critically low! Going to charge. The remaining tasks are: " + tasks_left);
                notifyCentralMonitor("charging-points");
                ACLMessage message = blockingReceive(2000);
                targetPoint = parsePoint(message.getContent());
                charging = true;
                needsCharging = true;
            }
            System.out.println("tasks: " + tasks);
            if (tasks.isEmpty()&& currentTask==null) {
                System.out.println("Tasks list empty! Going to charge.");
                notifyCentralMonitor("Tasks list empty! Going to charge.");
                notifyCentralMonitor("charging-points");
                ACLMessage message = blockingReceive(2000);
                if (message!=null) {
                    targetPoint = parsePoint(message.getContent().split("\n")[0]);
                }
                charging = true;
                needsCharging = false;
            }

            System.out.println("target: " + targetPoint + " distance: "+distanceToTarget);
            if (targetPoint != null){
                distanceToTarget = targetPoint.dist(getRobotLocation());
            }

            if (distanceToTarget <= 250) {      // 250 corresponds to the noise of UWB, we have chosen this number
                if (charging) {                 // if we reach the charging point
                    stopMotors();
                    notifyCentralMonitor("Robot currently charging in the charging station");
                    boolean charged = false;
                    while(!charged) {
                        charged = chargeBattery(needsCharging);
                    }
                    charging = false;
                    if (currentTask!=null) {
                        targetPoint = isPickup ? currentTask.getPickupPoint() : currentTask.getDropoffPoint();
                    }
                    return;
                } else {
                    // Check if we're at the triaging station
                    if (targetPoint!=null && targetPoint.equals(getTriagingStationLocation(getRobotLocation()))) {
                        System.out.println("Crate successfully dropped off at the triaging station.");
                        // Notify the central monitor
                        notifyCentralMonitor("Crate successfully dropped off at the triaging station.");
                    }

                    System.out.println((isPickup ? "Pickup" : "Dropoff") + " point reached: " + targetPoint);
                    notifyCentralMonitor((isPickup ? "Pickup" : "Dropoff") + " point reached: " + targetPoint);

                    if (isPickup) {
                        System.out.println("Checking for rotting crate");
                        checkRottingCrate();
                        isPickup = false;
                    } else {
                        if (!tasks.isEmpty()) {
                            if (tasksTime+RESTART_TASK_LIST_MS>=System.currentTimeMillis()) { // check if it hasn't been 5 min to update the task
                                do {
                                    currentTask = tasks.remove(0);
                                    if (currentTask.getDropoffTime().getTime() > System.currentTimeMillis() +4*60*1000){
                                        notifyCentralMonitor("Current task " + currentTask.toString() + "can't be completed in the selected time (4 min).");
                                    }
                                } while(!tasks.isEmpty() && currentTask.getDropoffTime().getTime() > System.currentTimeMillis() +4*60*1000);
                                isPickup = true;
                            } else {  //it's time to update the task list
                                isPickup = true;
                                notifyCentralMonitor(tasks.toString() + "not completed.");
                                addBehaviour(ReceiveTasksBehaviour);
                                //return;
                            }
                            notifyCentralMonitor("Current task:" + currentTask.toString());
                        } else {
                            currentTask = null;
                            notifyCentralMonitor("All task completed.");
                        }
                    }
                    stopMotors();
                    Delay.msDelay(3000);
                }
            } else {
                navigateToTarget();
            }
            if (currentTask!= null && !charging) {
                targetPoint = isPickup ? currentTask.getPickupPoint() : currentTask.getDropoffPoint();
            }
        }

        // USE CASE: TRANSFERING CRATES
        public double getUltrasonicDistance(EV3UltrasonicSensor sensor) {
            SampleProvider provider = sensor.getDistanceMode();
            float[] samples = new float[provider.sampleSize()];
            provider.fetchSample(samples, 0);
            return samples[0];
        }

        public Point2D getRobotLocation() {
            Point2D location = tag.getLocation();
            while (location == null) {
                location = tag.getLocation();
            }
            return location;
        }

        public void navigateToTarget() {
            frontDistance = (int) getUltrasonicDistance(frontSensor);
            if (frontDistance >= 20000000) {
                frontDistance = oldFrontDistance;
            }
            oldFrontDistance = frontDistance;
            int error = frontDistance - frontSafeDistance;
            int speed = Math.min(error * 10, 250);
            int leftDistance = (int) getUltrasonicDistance(leftSensor);
            int rightDistance = (int) getUltrasonicDistance(rightSensor);
            if (frontDistance <= frontSafeDistance + 5 && frontDistance >= frontSafeDistance - 5) {
                handleObstacles(leftDistance, rightDistance);
                avoidingObstacle = true;
            } else {
                if (!avoidingObstacle) {
                    alignToTarget(targetPoint);
                } else{
                    System.out.println("Avoiding obstacle");
                    if((leftDistance>=sideSafeDistance*3&&obstacleLeft) || (rightDistance>=sideSafeDistance*3&&!obstacleLeft)){
                        moveForward(speed);
                        Delay.msDelay(2000);
                        avoidingObstacle = false;
                    }
                }
                moveForward(speed);
            }
            leftDistance = (int) getUltrasonicDistance(leftSensor);
            rightDistance = (int) getUltrasonicDistance(rightSensor);
            maintainSideSafetyDistance(leftDistance, rightDistance);
            Delay.msDelay(200);
        }

        private void alignToTarget(Point2D targetPoint) {
            Point2D robotLocation = getRobotLocation();
            float currentAngle = tag.getAngle();
            float targetAngle = (float) Math.toDegrees(Math.atan2(
                    targetPoint.y - robotLocation.y,
                    targetPoint.x - robotLocation.x
            ));
            float angleDifference = normalizeAngle(targetAngle - currentAngle);
            while (Math.abs(angleDifference) > 20){
                currentAngle = tag.getAngle();
                targetAngle = (float) Math.toDegrees(Math.atan2(
                        targetPoint.y - robotLocation.y,
                        targetPoint.x - robotLocation.x
                ));
                angleDifference = normalizeAngle(targetAngle - currentAngle);
                if (angleDifference > 0) {
                    rotateRight();
                } else {
                    rotateLeft();
                }
                Delay.msDelay(50);
            }
        }

        private float normalizeAngle(float angle) {
            while (angle <= -180) angle += 360;
            while (angle > 180) angle -= 360;
            return angle;
        }

        // USE CASE: BATTERY CHARGING
        public void drainBattery(double amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Drain amount must be positive.");
            }
            simulatedBatteryLevel_ms = Math.max(0, simulatedBatteryLevel_ms - amount);
        }

        public boolean chargeBattery(boolean needsCharging) {
            long currentChargeTime = 0; // Tracks how long the battery has been charging
            long interval = 1000; // 1 second charging interval

            System.out.println("Charging started.");

            boolean alreadyCharged = false;

            while (currentChargeTime < TOTAL_BATTERY_LEVEL && !alreadyCharged) {
                // Simulate charging for the interval
                Delay.msDelay(interval);
                currentChargeTime += interval;

                // Check if it's time to notify the central monitor
                if (currentChargeTime >= (TOTAL_BATTERY_LEVEL - notifyTimeBeforeFinish)) {
                    System.out.println("Battery level 10 minutes from fully charged. Notifying the central monitor.");
                    notifyCentralMonitor("Battery level 10 minutes from fully charged. Notifying the central monitor.");
                    // Prevent notifying again
                    notifyTimeBeforeFinish = Long.MAX_VALUE;
                }

                // Simulate battery increment based on charging time
                simulatedBatteryLevel_ms = Math.min(TOTAL_BATTERY_LEVEL, simulatedBatteryLevel_ms + interval);
                System.out.println("Battery charging: " + simulatedBatteryLevel_ms + " ms.");

                if (TOTAL_BATTERY_LEVEL == simulatedBatteryLevel_ms) alreadyCharged = true;
            }
            System.out.println("Battery fully charged.");
            if (tasks.isEmpty()) addBehaviour(ReceiveTasksBehaviour);
            return true;
        }

        // USE CASE: TRIAGING ROTTING CRATES
        public void checkRottingCrate() {       // Check gas level and handle triaging
            double ethyleneLevel = simulateEthyleneLevel();
            notifyCentralMonitor("Ethylene level of the crate: " + ethyleneLevel);
            if (ethyleneLevel > ETHYLENE_THRESHOLD) {
                System.out.println("Crate marked as potentially rotting: Ethylene level = " + ethyleneLevel);

                // Notify the central monitor
                notifyCentralMonitor("Crate marked as potentially rotting. Redirecting to triaging station.");

                // Change the dropoff point to the triaging station
                currentTask.setDropoffPoint(getTriagingStationLocation(getRobotLocation()));
            } else {
                System.out.println("Crate not marked as potentially rotting");
            }
        }

        private double simulateEthyleneLevel() {
            return Math.random() * 100; // Simulate ethylene level in parts-per-million (ppm)
        }

        public Point2D getTriagingStationLocation(Point2D robotLocation) {
            return new Point2D(13007, 14219); // triaging station location
        }

        //USE CASE: COLLISION AVOIDANCE
        private void handleObstacles(int leftDistance, int rightDistance) {
            int frontDistance = (int) getUltrasonicDistance(frontSensor);

            while (frontDistance < frontSafeDistance + 40) {
                if (turnLeftNext && leftDistance > sideSafeDistance) {
                    rotateLeft();
                    obstacleLeft = false;
                    Delay.msDelay(100);
                } else if (!turnLeftNext && rightDistance > sideSafeDistance) {
                    rotateRight();
                    obstacleLeft = true;
                    Delay.msDelay(100);
                } else {
                    // If neither side is clear, stop and wait briefly before rechecking
                    stopMotors();
                    Delay.msDelay(9000);
                    System.out.println("Both sides blocked, pausing to reassess.");
                }

                // Update distances for continuous checking within the loop
                frontDistance = (int) getUltrasonicDistance(frontSensor);
                leftDistance = (int) getUltrasonicDistance(leftSensor);
                rightDistance = (int) getUltrasonicDistance(rightSensor);
            }
            turnLeftNext = !turnLeftNext;
            Delay.msDelay(50);
            moveForward(200);
        }

        private void maintainSideSafetyDistance(int leftDistance, int rightDistance) {
            if (leftDistance <= sideSafeDistance) {
                rotateRight();
                Delay.msDelay(200);
                obstacleLeft = true;
                avoidingObstacle = true;
            } else if (rightDistance <= sideSafeDistance) {
                rotateLeft();
                Delay.msDelay(200);
                obstacleLeft = false;
                avoidingObstacle = true;
            }
        }

        // ROBOT MOVEMENT
        private void stopMotors() {
            leftMotor.stop(true);
            rightMotor.stop(true);
        }

        private void rotateLeft() {
            leftMotor.setSpeed(100);
            rightMotor.setSpeed(100);
            leftMotor.backward();
            rightMotor.forward();
        }

        private void rotateRight() {
            leftMotor.setSpeed(100);
            rightMotor.setSpeed(100);
            leftMotor.forward();
            rightMotor.backward();
        }

        private void moveForward(int speed) {
            leftMotor.setSpeed(speed);
            rightMotor.setSpeed(speed);
            leftMotor.forward();
            rightMotor.forward();
        }

        public Point2D parsePoint(String pointString) {
            // Example format: "{x: 13165; y:14609}"
            pointString = pointString.replace("{x:", "").replace("}", "").replace("y:", "").trim();
            String[] coordinates = pointString.split(";");

            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Invalid point format: " + pointString);
            }

            int x = Integer.parseInt(coordinates[0].trim());
            int y = Integer.parseInt(coordinates[1].trim());
            return new Point2D(x, y);
        }
    };
}