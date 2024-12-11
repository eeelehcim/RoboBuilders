package Examples;

import lejos.robotics.Color;

public class ColorSensor {
    static boolean call_mode = false;

    public static float x = 0;
    public static int ReadColor() {
     if (!call_mode) {
         InitComps.colorSensor.getRGBMode();

         call_mode = true;
     }

        float colorSamplings = 0;
        int index = 0;
        while (index < 9) {
            int idcolor = InitComps.colorSensor.getColorID();
            colorSamplings += idcolor;
            index = index + 1;
        }
        x = (colorSamplings) / 10;
  return Math.round(x);  }
}
