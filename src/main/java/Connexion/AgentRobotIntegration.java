package Connexion;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.Battery;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import helpers.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static jade.lang.acl.ACLMessage.INFORM;

public class AgentRobotIntegration extends Agent {

    static EV3MediumRegulatedMotor leftMotor = new EV3MediumRegulatedMotor(MotorPort.B);
    static EV3MediumRegulatedMotor rightMotor = new EV3MediumRegulatedMotor(MotorPort.A);
    static EV3UltrasonicSensor frontSensor = new EV3UltrasonicSensor(SensorPort.S1);
    static EV3UltrasonicSensor rightSensor = new EV3UltrasonicSensor(SensorPort.S3);
    static EV3UltrasonicSensor leftSensor = new EV3UltrasonicSensor(SensorPort.S4);
    static TagIdMqtt tag;


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


    CyclicBehaviour robotControlBehaviour = new CyclicBehaviour() {
        int frontDistance = (int) getUltrasonicDistance(frontSensor);
        int frontSafeDistance = 20;
        int oldFrontDistance = frontSafeDistance + 20;
        double distanceToTarget = 0;
        boolean turnLeftNext = true;
        int sideSafeDistance = 5;
        Point2D targetPoint;

        @Override
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                System.out.println("action: " + message);
                String actionMessage = message.getContent();

                if (!actionMessage.equals("stop")) {
                    stopMotors();
                } else {
                    targetPoint = parsePoint(actionMessage);
                    distanceToTarget = targetPoint.dist(getRobotLocation());
                    System.out.println("target: " + targetPoint + " distance: " + distanceToTarget);
                    navigateToTarget();
                }

                if (distanceToTarget <= 250) { // 250 corresponds to the noise of UWB
                    stopMotors();
                    notifyCentralMonitor("Robot reached the target point");
                    Delay.msDelay(3000); // Pause before resuming
                }
                block(100); // Add delay to simulate periodic execution, similar to TickerBehaviour
            }
        }

        public Point2D parsePoint(String pointString) {
            pointString = pointString.replace("{x:", "").replace("}", "").replace("y:", "").trim();
            String[] coordinates = pointString.split(";");

            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Invalid point format: " + pointString);
            }

            int x = Integer.parseInt(coordinates[0].trim());
            int y = Integer.parseInt(coordinates[1].trim());
            return new Point2D(x, y);
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
            } else {
                alignToTarget(targetPoint);
                moveForward(speed);
            }

            leftDistance = (int) getUltrasonicDistance(leftSensor);
            rightDistance = (int) getUltrasonicDistance(rightSensor);
            maintainSideSafetyDistance(leftDistance, rightDistance);

            Delay.msDelay(200);
        }

        private void handleObstacles(int leftDistance, int rightDistance) {
            int frontDistance = (int) getUltrasonicDistance(frontSensor);

            while (frontDistance < frontSafeDistance + 20) {
                if (turnLeftNext && leftDistance > sideSafeDistance) {
                    rotateLeft();
                    Delay.msDelay(100);
                } else if (!turnLeftNext && rightDistance > sideSafeDistance) {
                    rotateRight();
                    Delay.msDelay(100);
                } else {
                    stopMotors();
                    Delay.msDelay(9000); // Pause briefly before rechecking
                    System.out.println("Both sides blocked, pausing to reassess.");
                }

                // Update distances for continuous checking
                frontDistance = (int) getUltrasonicDistance(frontSensor);
                leftDistance = (int) getUltrasonicDistance(leftSensor);
                rightDistance = (int) getUltrasonicDistance(rightSensor);
            }
            Delay.msDelay(50);
            moveForward(200);
            Delay.msDelay(3000);
            stopMotors();
        }

        private void alignToTarget(Point2D targetPoint) {
            Point2D robotLocation = getRobotLocation();
            float currentAngle = tag.getAngle();
            float targetAngle = (float) Math.toDegrees(Math.atan2(
                    targetPoint.y - robotLocation.y,
                    targetPoint.x - robotLocation.x
            ));
            float angleDifference = normalizeAngle(targetAngle - currentAngle);

            while (Math.abs(angleDifference) > 20) {
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

        private float normalizeAngle(float angle) {
            while (angle <= -180) angle += 360;
            while (angle > 180) angle -= 360;
            return angle;
        }

        private void maintainSideSafetyDistance(int leftDistance, int rightDistance) {
            if (leftDistance <= sideSafeDistance) {
                rotateRight();
                Delay.msDelay(200);
            } else if (rightDistance <= sideSafeDistance) {
                rotateLeft();
                Delay.msDelay(200);
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
    };
}


