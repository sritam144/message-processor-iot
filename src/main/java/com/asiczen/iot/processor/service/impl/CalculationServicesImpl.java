package com.asiczen.iot.processor.service.impl;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.asiczen.iot.processor.model.TransformedMessage;
import com.asiczen.iot.processor.service.CalculationServices;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CalculationServicesImpl implements CalculationServices {

    private static final double AVERAGE_RADIUS_OF_EARTH = 6371;
    private static final double DISTANCE_CORRECTION = 12.0d; // In meters
    private static final double DISTANCE_PERCENT_CORRECTION = 0.75d;

    @Override
    public double calculateSpeedInKmHour(double distanceinMeter, int second) {

        if (second == 0) {
            return 0d;
        }

        if (distanceinMeter <= 0) {
            return 0d;
        }

        return (distanceinMeter / second) * 3.6d;
    }

    @Override
    public double calculateDistanceInKM(TransformedMessage locaton1, TransformedMessage location2) {

        double lon1 = Math.toRadians(locaton1.getLng());
        double lon2 = Math.toRadians(location2.getLng());
        double lat1 = Math.toRadians(locaton1.getLat());
        double lat2 = Math.toRadians(location2.getLat());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Multiply 1.609344 to get distance in KM
        return Math.abs((c * AVERAGE_RADIUS_OF_EARTH * 1.609344d) * (DISTANCE_PERCENT_CORRECTION));
    }

    @Override
    public int differenceBetweenTwoTimeStampsInSeconds(Date timeStampFrom, Date timeStampTo) {
        return (int) Math.abs(TimeUnit.MILLISECONDS.toSeconds((timeStampFrom.getTime() - timeStampTo.getTime())));
    }

    @Override
    public void updateStationaryVehicleCoOrdinates(TransformedMessage transformedMessage, TransformedMessage oldMessage,
                                                   double distanceInKm) {

        if (distanceInKm * 1000d <= DISTANCE_CORRECTION) {

            transformedMessage.setLat(oldMessage.getLat());
            transformedMessage.setLng(oldMessage.getLng());
            transformedMessage.setCalulatedDistance(0);
            transformedMessage.setCalculatedDailyDistance(oldMessage.getCalculatedDailyDistance());
            // else update the speed, distance, total distance
        } else {
            transformedMessage.setCalulatedDistance(distanceInKm);
            transformedMessage.setCalculatedDailyDistance(oldMessage.getCalculatedDailyDistance() + distanceInKm);
        }

    }

    // Method checks if engine is On and vechile is not moving then set idleEngineOn
    // idleKeyOnTime

    @Override
    public void checkEngineOnVehicleIdle(TransformedMessage transformedMessage, TransformedMessage oldMessage,
                                         double distanceInKm) {

        // Engine is on and distance traveled is 0 then flag is set to true
        if (transformedMessage.isKeyOn() && ((distanceInKm * 1000d) < DISTANCE_CORRECTION)) {
            transformedMessage.setIdleEngineOn(true);
            int timeDifference = Math.abs(differenceBetweenTwoTimeStampsInSeconds(oldMessage.getTimestamp(),
                    transformedMessage.getTimestamp()));
            transformedMessage.setIdleKeyOnTime(oldMessage.getIdleKeyOnTime() + timeDifference);
        } else {
            transformedMessage.setIdleKeyOnTime(oldMessage.getIdleKeyOnTime());
        }
    }

    @Override
    public void checkForTopSpeed(TransformedMessage transformedMessage, double oldMaxSpeed, double currentSpeed) {
        transformedMessage.setTopSpeed((currentSpeed > oldMaxSpeed) ? currentSpeed : oldMaxSpeed);
    }

    @Override
    public void calculateAvgSpeed(TransformedMessage transformedMessage, double oldAvgSpeed) {
        int messageCounter = transformedMessage.getMessageCounter();

        if (messageCounter == 0) {
            transformedMessage.setAverageSpeed(0d);
        } else {
            transformedMessage.setAverageSpeed((oldAvgSpeed + transformedMessage.getCalculatedSpeed()) / messageCounter);
        }

    }

    @Override
    public void checkEngineOffAndVehicleNotMoving(TransformedMessage transformedMessage, TransformedMessage oldMessage,
                                                  double distanceInKm) {

        if (!transformedMessage.isKeyOn() && ((distanceInKm * 1000d) < DISTANCE_CORRECTION)) {
            transformedMessage.setIdleEngineOff(true);
            int timeDifference = Math.abs(differenceBetweenTwoTimeStampsInSeconds(oldMessage.getTimestamp(),
                    transformedMessage.getTimestamp()));
            transformedMessage.setIdleKeyOffTime(oldMessage.getIdleKeyOffTime() + timeDifference);
        } else {
            transformedMessage.setIdleKeyOffTime(oldMessage.getIdleKeyOffTime());
        }

    }

    @Override
    public void checkEngineOnAndVehicleMovingTime(TransformedMessage transformedMessage, TransformedMessage oldMessage,
                                                  double distanceInKm) {

        if (transformedMessage.isKeyOn() && ((distanceInKm * 1000d) > DISTANCE_CORRECTION)) {
            transformedMessage.setVehicleMovingFlag(true);
            int timeDifference = Math.abs(differenceBetweenTwoTimeStampsInSeconds(oldMessage.getTimestamp(),
                    transformedMessage.getTimestamp()));
            transformedMessage.setVehicleMovingTime(oldMessage.getVehicleMovingTime() + timeDifference);
        } else {
            transformedMessage.setVehicleMovingTime(oldMessage.getVehicleMovingTime());
        }

    }

    @Override
    public void updateCalculatedDailyDistanceAndDistance(TransformedMessage transformedMessage, TransformedMessage oldMessage, double distanceInKm) {

        if ((distanceInKm * 1000d) > DISTANCE_CORRECTION) {
            transformedMessage.setCalculatedDailyDistance(oldMessage.getCalculatedDailyDistance() + distanceInKm);
            transformedMessage.setCalulatedDistance(distanceInKm);
        } else {
            transformedMessage.setCalculatedDailyDistance(oldMessage.getCalculatedDailyDistance());
            transformedMessage.setCalulatedDistance(0d);
        }
    }

    @Override
    public void updateCalculatedSpeed(TransformedMessage transformedMessage, double calculatedSpeed) {

        if (calculatedSpeed > 10) {
            transformedMessage.setVehicleMovingFlag(true);
            transformedMessage.setCalculatedSpeed(calculatedSpeed);
        } else {
            transformedMessage.setCalculatedSpeed(0.0d);
        }


    }

}
