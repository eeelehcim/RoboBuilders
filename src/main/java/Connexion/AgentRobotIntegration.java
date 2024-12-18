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
import jade.lang.acl.MessageTemplate;
//import jade.lang.acl.ParseException;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.ParseException;
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
    private Point2D targetPoint;

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
        addBehaviour(initMessage);
        addBehaviour(MessageReceiverBehaviour);
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


    CyclicBehaviour NavigateToTarget = new CyclicBehaviour() {

        int frontDistance = (int) getUltrasonicDistance(frontSensor);
        int frontSafeDistance = 20;
        int oldFrontDistance = frontSafeDistance + 20;
        boolean turnLeftNext = true;
        int sideSafeDistance = 5;

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

        private void handleObstacles(int leftDistance, int rightDistance) {
            int frontDistance = (int) getUltrasonicDistance(frontSensor);

            while (frontDistance < frontSafeDistance + 20) {
                frontDistance = (int) getUltrasonicDistance(frontSensor);
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

            while (Math.abs(angleDifference) > 15) {
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


        @Override
        public void action() {
            Point2D robotLocation = getRobotLocation();
            double distanceToTarget = robotLocation.dist(targetPoint);
            System.out.println("navigateToTarget:\t" + targetPoint + "\nDistance:\t" +distanceToTarget);
            if (distanceToTarget <= 250) { // 250 corresponds to the noise of UWB
                stopMotors();
                System.out.println("Arrived");
                Delay.msDelay(3000); // Pause before resuming
                removeBehaviour(NavigateToTarget);
                return;
            }
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
    };

    CyclicBehaviour MessageReceiverBehaviour = new CyclicBehaviour() {
        @Override
        public void action() {
            //stopMotors();
            MessageTemplate pathTemplate = MessageTemplate.and(
                    MessageTemplate.MatchOntology("source_target_line_string"),
                    MessageTemplate.MatchConversationId("line_string")
            );

            ACLMessage pathMsg = myAgent.receive(pathTemplate);
            if (pathMsg != null && pathMsg.getPerformative() == ACLMessage.INFORM) {
                System.out.println("Received Path Message from " + pathMsg.getSender().getLocalName());
                String pathString = pathMsg.getContent();
                System.out.println("Path Content: " + pathString);

                try {
                    // Parse the received path
                    WKTReader reader = new WKTReader();
                    LineString pathLineString = (LineString) reader.read(pathString);
                    handlePath(pathLineString);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            MessageTemplate stopTemplate = MessageTemplate.and(
                    MessageTemplate.MatchOntology("collision_line_string"),
                    MessageTemplate.MatchConversationId("line_string"));
            ACLMessage stopMsg = myAgent.receive(stopTemplate);

            if (stopMsg != null && stopMsg.getPerformative() == ACLMessage.INFORM) {
                System.out.println("Received Stop Message from " + stopMsg.getSender().getLocalName());
                String contents = stopMsg.getContent();
                System.out.println("Stop Content: " + contents);
                removeBehaviour(NavigateToTarget);
                stopMotors(); // replace with your own stop functionality
            }

            block(20);
        }

        private void stopMotors() {
            leftMotor.stop(true);
            rightMotor.stop(true);
        }
        private void handlePath(LineString pathLineString) {
            System.out.println("Handling Path: " + pathLineString);
            Point2D startPoint = new Point2D((int) pathLineString.getStartPoint().getX(),
                    (int) pathLineString.getStartPoint().getY());
            Point2D endPoint = new Point2D((int) pathLineString.getEndPoint().getX(),
                    (int) pathLineString.getEndPoint().getY());
            System.out.println("Moving from " + startPoint + " to " + endPoint);

            // Start moving to the target
            removeBehaviour(NavigateToTarget);
            targetPoint = endPoint;
            addBehaviour(NavigateToTarget);
        }
    };
}