package Examples;

import lejos.utility.Delay;

public class Motor {
    public static void run1() {
        InitComps.motor.setSpeed(250);
        Delay.msDelay(1);
        InitComps.motor.forward();
        Delay.msDelay(3000);
        InitComps.motor.stop();
    }
    public static void run2() {
        InitComps.motor.setSpeed(500);
        Delay.msDelay(1);
        InitComps.motor.forward();
        Delay.msDelay(3000);
        InitComps.motor.stop();
    }
    public static void run3() {
        InitComps.motor.setSpeed(750);
        Delay.msDelay(1);
        InitComps.motor.forward();
        Delay.msDelay(3000);
        InitComps.motor.stop();
    }
    public static void run4() {
        InitComps.motor.setSpeed(950);
        Delay.msDelay(1);
        InitComps.motor.forward();
        Delay.msDelay(3000);
        InitComps.motor.stop();
    }
}
