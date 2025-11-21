package org.firstinspires.ftc.teamcode.utils;

public class Odometry {

    // gobilda 312 rpm (19.2:1) motor encoder spec
    public static final double TICKS_PER_REV = 537.7;

    // 4-inch wheel
    public static final double WHEEL_DIAMETER_IN = 4.0;

    // precompute circumference
    public static final double WHEEL_CIRCUMFERENCE_IN = Math.PI * WHEEL_DIAMETER_IN;

    /**
     * Convert encoder ticks to linear distance (in inches)
     */
    public static double ticksToInches(int ticks) {
        return (ticks / TICKS_PER_REV) * WHEEL_CIRCUMFERENCE_IN;
    }

    /**
     * Convert distance (in inches) → ticks
     */
    public static int inchesToTicks(double inches) {
        return (int) Math.round((inches / WHEEL_CIRCUMFERENCE_IN) * TICKS_PER_REV);
    }
}
