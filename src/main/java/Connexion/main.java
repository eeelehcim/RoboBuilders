package Connexion;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args) throws MqttException, InterruptedException {
        TagIdMqtt tag = new TagIdMqtt("6841");

        while (true) {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("tag.getLocation() = " + tag.getSmoothenedLocation(20));
            System.out.println(tag.getAngle());
        }
    }
}