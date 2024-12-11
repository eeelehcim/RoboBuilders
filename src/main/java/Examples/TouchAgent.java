package Examples;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import lejos.utility.Delay;

import static jade.lang.acl.ACLMessage.INFORM;

public class TouchAgent extends Agent{
    static boolean press = false;
    @Override
    protected void setup() {
        addBehaviour(new Behaviour() {
            @Override
            public void action() {
                /** If button is pressed, send message to Color Sensor */
                if (Devices.isPressed()) {
                    press = true;
                    Delay.msDelay(1500);
                    ACLMessage messageTemplate = new ACLMessage(INFORM);
                    messageTemplate.addReceiver(new AID("ColorSensorAgent", AID.ISLOCALNAME));
                    messageTemplate.setContent("Start reading color");
                    send(messageTemplate);
                   // System.out.println(messageTemplate.getContent());
             }}

            @Override
            public boolean done() {     /** stop condition of the simple behaviour**/
                if (press)
                    return true;
                else
                    return  false;
            }
        });

    }

    @Override
    protected void takeDown() {
    }
}
