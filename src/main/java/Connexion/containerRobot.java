package Connexion;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class containerRobot {
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();
            String target ="192.168.0.161";
            String source ="192.168.0.171";
            ProfileImpl p = new ProfileImpl(target,1099,null,false);

            p.setParameter(Profile.LOCAL_HOST,source);
            p.setParameter(Profile.LOCAL_PORT,"1099");

            AgentContainer agentContainer=runtime.createAgentContainer(p);
            containerRobot.start();

            AgentController agentRobot=agentContainer.createNewAgent("AgentRobot",
                    "Connexion.AgentRobot",new Object[]{});
            agentRobot.start();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void start() {
    }
}
