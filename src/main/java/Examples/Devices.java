package Examples;

public class Devices {

    public static int IdColor() {
        while (true)
   return      ColorSensor.ReadColor();

    }
    public static boolean isPressed() {
        return Touch.touch();
    }
    public static void Run1(){
        Motor.run1();
    }
    public static void Run2(){
        Motor.run2();
    }
    public static void Run3(){
        Motor.run3();
    }
    public static void Run4(){
        Motor.run4();
    }
}
