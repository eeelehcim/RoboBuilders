package Connexion;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class containerRobotIntegration {
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();
            String target ="192.168.0.129";             // collision avoidance integration
            String source ="192.168.0.171";
            ProfileImpl p = new ProfileImpl(target,1099,null,false);

            p.setParameter(Profile.LOCAL_HOST,source);
            p.setParameter(Profile.LOCAL_PORT,"1099");
            p.setParameter(Profile.CONTAINER_NAME,"collision");             // collision avoidance integration

            AgentContainer agentContainer=runtime.createAgentContainer(p);
            containerRobotIntegration.start();

            AgentController agentRobot=agentContainer.createNewAgent("AgentRobot", "Connexion.AgentRobot",new Object[]{});
            agentRobot.start();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void start() {

    }
}
