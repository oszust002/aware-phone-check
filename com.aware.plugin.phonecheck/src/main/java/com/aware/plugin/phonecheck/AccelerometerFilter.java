package com.aware.plugin.phonecheck;

public class AccelerometerFilter {
    private final float timeConstant;
    private float[] lowpassOutput = new float[]{0, 0, 0};
    private long lowpassStartTime;
    private int lowpassCount;

    public AccelerometerFilter(float timeConstant) {
        this.timeConstant = timeConstant;
    }

    public float[] calculate(float[] rawAcceleration) {
        float[] gravity = lowpass(rawAcceleration);
        float[] output = new float[3];
        output[0] = rawAcceleration[0] - gravity[0];
        output[1] = rawAcceleration[1] - gravity[1];
        output[2] = rawAcceleration[2] - gravity[2];
        return output;
    }

    private float[] lowpass(float[] rawAcceleration) {

        // Initialize the start time.
        if (lowpassStartTime == 0) {
            lowpassStartTime = System.nanoTime();
        }

        long lowpassTimestamp = System.nanoTime();

        // Find the sample period (between updates) and convert from
        // nanoseconds to seconds. Note that the sensor delivery rates can
        // individually vary by a relatively large time frame, so we use an
        // averaging technique with the number of sensor updates to
        // determine the delivery rate.
        float dt = 1 / (lowpassCount++ / ((lowpassTimestamp - lowpassStartTime) / 1000000000.0f));

        float alpha = timeConstant / (timeConstant + dt);

        lowpassOutput[0] = alpha * lowpassOutput[0] + (1 - alpha) * rawAcceleration[0];
        lowpassOutput[1] = alpha * lowpassOutput[1] + (1 - alpha) * rawAcceleration[1];
        lowpassOutput[2] = alpha * lowpassOutput[2] + (1 - alpha) * rawAcceleration[2];

        return lowpassOutput;
    }
}
