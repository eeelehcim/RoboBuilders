package Examples;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import lejos.utility.Delay;

public class MotorAgent extends Agent{
    @Override
    public void setup() {
        addBehaviour(new  CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage message=receive();
                if (message!=null) {

                    System.out.println("Motor Agent: "+message.getContent());
                    if (message.getContent().equals("Blue")) {
                        System.out.println("Motor run with 250 degree/s");
                        Devices.Run1();
                        restartread();
                    }
                    if (message.getContent().equals("Green")) {

                        System.out.println("Motor run with 500 degree/s");
                        Devices.Run2();
                        restartread();
                    }
                    if (message.getContent().equals("Yellow")) {

                        System.out.println("Motor run with 750 degree/s");
                        Devices.Run3();
                        restartread();
                    }
                    if (message.getContent().equals("Red")) {

                        System.out.println("Motor run with 950 degree/s");
                        Devices.Run4();
                        restartread();
                    }
                 }

            }
        });
    }
    public  void restartread() {
        Delay.msDelay(1500);
        ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
        msg1.addReceiver(new AID("ColorSensorAgent", AID.ISLOCALNAME));
        msg1.setContent("re-read color");
        send(msg1);
        System.out.println("eeeeeee");
    }
}
