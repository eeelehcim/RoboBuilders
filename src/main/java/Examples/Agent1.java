package Examples;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class Agent1 extends  Agent{
    @Override
    public void setup() {
        addBehaviour(new TickerBehaviour(this, 20000) {
            @Override
            protected void onTick() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("Agent2", AID.ISLOCALNAME));
                msg.setContent("Hello");
                msg.setLanguage("English");
                msg.setOntology("Salute");
                System.out.println(msg.getContent());
                send(msg);

            }
        });
    }
}
