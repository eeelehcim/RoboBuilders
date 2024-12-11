package Examples;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3ColorSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

public class InitComps {
   static EV3ColorSensor colorSensor = null;
  static   EV3TouchSensor touch = null;
   static EV3LargeRegulatedMotor motor = null;
    public static void configuration() {
        colorSensor = new EV3ColorSensor(SensorPort.S1);
        touch = new EV3TouchSensor(SensorPort.S2);
        motor= new EV3LargeRegulatedMotor(MotorPort.A);
    }
}
