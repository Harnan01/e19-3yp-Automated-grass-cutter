package com.example.grasscutter.IoT;

import com.amazonaws.services.iot.client.*;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MQTTConfig {

    @Autowired
    private MongoTemplate mongoTemplate;
    String clientEndpoint = "a2ol1u6jexvsgb-ats.iot.ap-south-1.amazonaws.com";
    String awsAccessKeyId = "AKIAS6B2C4XUM3GPLPND";
    String awsSecretAccessKey ="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";
    private List<AngleDistancePair> temporaryData = new ArrayList<>();
    private boolean addingData = true;


    public void stopAddingData() {
        addingData = false;
        saveToMongo(temporaryData);
        temporaryData.clear();
        addingData =true;
    }

    public void subscribeToTopic(String topic, String clientId) throws AWSIotException {
        // Make sure the client is connected before subscribing
        AWSIotMqttClient subscriptionClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, null);

        subscriptionClient.connect();

        // Create a new AWSIotTopic object
        AWSIotTopic iotTopic = new AWSIotTopic(topic, AWSIotQos.QOS1) {
            @Override
            public void onMessage(AWSIotMessage message) {
                if (addingData) {
                    double angle = parseAngleFromPayload(message.getStringPayload());
                    double distance = parseDistanceFromPayload(message.getStringPayload());

                    // Create an AngleDistancePair and store it in the list
                    AngleDistancePair pair = new AngleDistancePair(angle, distance);
                    temporaryData.add(pair);
                }
            }
        };
        subscriptionClient.subscribe(iotTopic, true);
    }

    private void saveToMongo(List<AngleDistancePair> data) {
        try {
            // Create an IoTData object and set the list of pairs
            IoTData iotData = new IoTData();
            iotData.setAngleDistancePairs(data);

            // Save the IoTData object to MongoDB
            mongoTemplate.save(iotData);

            System.out.println("Saved to MongoDB: " + data.toString());
        } catch (Exception e) {
            e.printStackTrace(); // Log or handle the exception appropriately
            System.err.println("Failed to save to MongoDB");
        }
    }


//    private void saveToMongo(List<AngleDistancePair> data) {
//        try {
//            // Iterate through the data list and save each entry to MongoDB
//            for (AngleDistancePair pair : data) {
//                // Assume a simple entity called IoTData
//                IoTData iotData = new IoTData(pair); // Assuming IoTData has a constructor that takes AngleDistancePair
//                mongoTemplate.save(iotData);
//                System.out.println("Saved to MongoDB: " + pair.toString());
//            }
//        } catch (Exception e) {
//            e.printStackTrace(); // Log or handle the exception appropriately
//            System.err.println("Failed to save to MongoDB");
//        }
//    }

//    private void saveToMongo( String payload) {
//        try {
//            // Assume a simple entity called IoTData
//            IoTData iotData = new IoTData(payload);
//            mongoTemplate.save(iotData);
//            System.out.println("Saved to MongoDB: " + payload);
//        } catch (Exception e) {
//            e.printStackTrace(); // Log or handle the exception appropriately
//            System.err.println("Failed to save to MongoDB: " + payload);
//        }
//    }

    private double parseAngleFromPayload(String payload) {
        try {
            // Parse the JSON payload
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(payload);

            // Extract the angle from the JSON
            JsonNode angleNode = rootNode.path("Angle");
            if (angleNode.isNumber()) {
                return angleNode.asDouble();
            } else {
                // Handle the case where the "Angle" field is not a number
                throw new IllegalArgumentException("Invalid angle value in payload");
            }
        } catch (Exception e) {
            // Handle any exceptions that might occur during parsing
            e.printStackTrace();
            throw new IllegalArgumentException("Error parsing angle from payload");
        }
    }

    private double parseDistanceFromPayload(String payload) {
        try {
            // Parse the JSON payload
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(payload);

            // Extract the distance from the JSON
            JsonNode distanceNode = rootNode.path("Distance");
            if (distanceNode.isNumber()) {
                return distanceNode.asDouble();
            } else {
                // Handle the case where the "Distance" field is not a number
                throw new IllegalArgumentException("Invalid distance value in payload");
            }
        } catch (Exception e) {
            // Handle any exceptions that might occur during parsing
            e.printStackTrace();
            throw new IllegalArgumentException("Error parsing distance from payload");
        }
    }

}