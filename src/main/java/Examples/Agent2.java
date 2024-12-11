package Examples;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import lejos.utility.Delay;

public class Agent2 extends Agent {
    @Override
    public void setup() {
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage Message = receive();
                if (Message!=null) {
                    System.out.println("Message content: "+Message.getContent());
                    System.out.println("Message Protocol: "+Message.getProtocol());
                    System.out.println("Language : "+Message.getLanguage());
                    System.out.println("Ontology: "+Message.getOntology());
                    Delay.msDelay(2000);
                  ACLMessage reply =  Message.createReply();
                  reply.setContent("Hi !");
                  send(reply);
                }
                else block();
            }
        });
    }
}
