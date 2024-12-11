package Examples;

//import Agents.ContainerRobot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

public class SimpleContainer {
    public static void main(String[] args) {

        try {
            Runtime runtime=Runtime.instance();
            Profile profile=new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            AgentContainer agentContainer=runtime.createAgentContainer(profile);
            agentContainer.start();
            AgentController Agent=agentContainer.createNewAgent("Agent2",
                    "Examples.Agent2",new Object[]{});
            Agent.start();
        } catch (ControllerException e) {
            e.printStackTrace();
        }


    }

    private static void start() {
    }
}
