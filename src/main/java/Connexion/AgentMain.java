package Connexion;

import helpers.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.util.*;

import static jade.lang.acl.ACLMessage.INFORM;


public class AgentMain extends Agent {
    Point2D[] pickup_points;
    Point2D[] dropoff_points;
    Point2D[] charging_points;
    Point2D triaging_station;
    Point2D robotLocation;

    @Override
    protected void setup() {
        dropoff_points = new Point2D[2];
        dropoff_points[0] = new Point2D(13165, 14609);
        dropoff_points[1] = new Point2D(13700, 14140);
        pickup_points = new Point2D[2];
        pickup_points[0] = new Point2D(14074, 15150);
        pickup_points[1] = new Point2D(11943, 15009);
        charging_points = new Point2D[2];
        charging_points[0] = new Point2D(11640, 14469);
        charging_points[1] = new Point2D(13074, 15079);
        triaging_station = new Point2D(13007, 14219);

        System.out.println("Local name Main " + getAID().getLocalName());
        System.out.println("Global name Main " + getAID().getName());
        addBehaviour(receiveMessages);
    }

    CyclicBehaviour receiveMessages = new CyclicBehaviour() {
        @Override
        public void action() {
            try {
                ACLMessage Message = receive();
                if (Message != null) {
                    String[] content = Message.getContent().split("\\s+");
                    if (content[0].equals("location")) {
                        robotLocation = new Point2D(Integer.parseInt(content[1]), Integer.parseInt(content[2]));
                    } else if (content[0].equals("request-tasks")) {
                        addBehaviour(sendTasksBehaviour);
                    } else if (content[0].equals("charging-points")) {
                        addBehaviour(sendChargingPoints);
                    }
                    System.out.println("Agent Main Received: " + Message.getContent());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
        }
    };

    public Task generateTask() {
        Random rand = new Random();
        long currentTime = System.currentTimeMillis();
        Timestamp pickupTime = new Timestamp(currentTime + rand.nextInt(10000)); // Current time + random offset
        Timestamp dropoffTime = new Timestamp(pickupTime.getTime() + rand.nextInt(10000)); // After pickup time

        return new Task(pickupTime, dropoffTime, dropoff_points[rand.nextInt(2)], pickup_points[rand.nextInt(2)]);
    }

    public static String sortedTasksToString(Task[] tasks) {
        StringBuilder result = new StringBuilder();
        for (Task task : tasks) {
            result.append(task.toString()).append("\n");
        }
        return result.toString();
    }

    OneShotBehaviour sendTasksBehaviour = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                // Generate a list of tasks
                Task[] tasks = new Task[10];
                for (int i = 0; i < 10; i++) {       // Generate 10 tasks
                    tasks[i] = generateTask();
                }

                // Format tasks as a message
                StringBuilder messageContent = new StringBuilder();
                for (Task task : tasks) {
                    messageContent.append(task.toString()).append("\n");
                }
                Task[] sorted_tasks = sortTasks(robotLocation, tasks);
                String sorted_tasks_to_string = sortedTasksToString(sorted_tasks);
                System.out.println(sorted_tasks_to_string);

                // Create and send the message to AgentRobot
                ACLMessage message = new ACLMessage(INFORM);
                message.addReceiver(new AID("AgentRobot", AID.ISLOCALNAME)); // Target AgentRobot
                message.setContent(messageContent.toString());
                send(message);

                System.out.println("Tasks sent to AgentRobot:\n" + messageContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static Task[] sortTasks(Point2D robotLocation, Task[] tasks) {
        Arrays.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                // Compare by earliest pickup time
                int pickupComparison = t1.getPickupTime().compareTo(t2.getPickupTime());
                if (pickupComparison != 0) {
                    return pickupComparison;
                }

                if (robotLocation != null) {
                    // If pickup times are the same, compare by distance to the pickup point
                    double distanceToT1 = robotLocation.dist(t1.getPickupPoint());
                    double distanceToT2 = robotLocation.dist(t2.getPickupPoint());
                    return Double.compare(distanceToT1, distanceToT2);
                }

                // If pickup times and distances are the same
                return 0;
            }
        });
        return tasks;
    }

    OneShotBehaviour sendChargingPoints = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                ACLMessage message = new ACLMessage(INFORM);
                message.addReceiver(new AID("AgentRobot", AID.ISLOCALNAME)); // Target AgentRobot
                Point2D chargingPointNearest = nearestChargingPoint(robotLocation);
                System.out.println("Sending: " + chargingPointNearest.toString());
                message.setContent(chargingPointNearest.toString());
                send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public Point2D nearestChargingPoint(Point2D robotLocation) {
        if (robotLocation.dist(charging_points[0]) > robotLocation.dist(charging_points[1])) {
            return charging_points[1];
        } else return charging_points[0];
    }
}
