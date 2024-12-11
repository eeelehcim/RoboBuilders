package Examples;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class MainContainer {
    /**
      If you want to run the main container with a SimpleContainer you have to
     . comment  MotorAgent, TouchAgent and ColorSensorAgent line 28 to 36
     . de-comment  Agent1 line 25 to line 27
     . Open the GUI by switching false to true (line 20) and in the start function comment  InitComps.configuration(); line 46
     . Run MainContainer then SimpleContainer.
     **/
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();
            Properties properties = new ExtendedProperties();
            properties.setProperty(Profile.GUI, "false");

            Profile profile = new ProfileImpl(properties);
            AgentContainer agentContainer=runtime.createMainContainer(profile);
            MainContainer.start();
          /*  AgentController Agent=agentContainer.createNewAgent("Agent1",
                    "Examples.Agent1",new Object[]{});
            Agent.start();*/
           AgentController MotorAgent=agentContainer.createNewAgent("MotorAgent",
                    "Examples.MotorAgent",new Object[]{});
            AgentController TouchAgent=agentContainer.createNewAgent("TouchAgent",
                    "Examples.TouchAgent",new Object[]{});
            AgentController ColorSensorAgent=agentContainer.createNewAgent("ColorSensorAgent",
                    "Examples.ColorSensorAgent",new Object[]{});
            MotorAgent.start();
            TouchAgent.start();
            ColorSensorAgent.start();

//
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void start() {
        InitComps.configuration();
    }
}
