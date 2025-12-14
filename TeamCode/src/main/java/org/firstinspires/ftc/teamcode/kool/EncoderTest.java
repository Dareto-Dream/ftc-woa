package org.firstinspires.ftc.teamcode.kool;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * EncoderTest - Comprehensive encoder and IMU testing
 *
 * CONTROLS:
 * - DPAD UP: Drive forward 12 inches
 * - DPAD DOWN: Drive backward 12 inches
 * - DPAD LEFT: Strafe left 12 inches
 * - DPAD RIGHT: Strafe right 12 inches
 * - LEFT BUMPER: Rotate 90° counterclockwise
 * - RIGHT BUMPER: Rotate 90° clockwise
 * - Y: Reset all encoders to zero
 * - X: Toggle continuous encoder display
 * - A: Test drive to specific position
 * - B: Stop all motors
 *
 * DISPLAY:
 * - Shows all four wheel encoder values
 * - Shows calculated robot position (X, Y, Rotation)
 * - Shows IMU heading
 * - Shows encoder ticks per inch calibration
 */
@TeleOp(name = "Encoder Test", group = "Testing")
public class EncoderTest extends LinearOpMode {

    // Motors
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private IMU imu;

    // Encoder tracking
    private int flStart = 0, frStart = 0, blStart = 0, brStart = 0;

    // Robot constants (from AutoPathFollower)
    private static final double TICKS_PER_INCH = 29.9;  // Encoder ticks per inch
    private static final double WHEEL_DIAMETER = 4.094; // inches
    private static final double TICKS_PER_REV = 384.5;    // Encoder ticks per revolution

    // Movement parameters
    private static final double DRIVE_SPEED = 0.5;
    private static final double TURN_SPEED = 0.3;

    // Display mode
    private boolean continuousDisplay = true;

    @Override
    public void runOpMode() {

        // Initialize hardware
        telemetry.addData("Status", "Initializing hardware...");
        telemetry.update();

        try {
            frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
            frontRight = hardwareMap.get(DcMotor.class, "frontRight");
            backLeft = hardwareMap.get(DcMotor.class, "backLeft");
            backRight = hardwareMap.get(DcMotor.class, "backRight");
            imu = hardwareMap.get(IMU.class, "imu");

            // Set motor directions
            frontLeft.setDirection(DcMotor.Direction.REVERSE);
            backLeft.setDirection(DcMotor.Direction.REVERSE);
            frontRight.setDirection(DcMotor.Direction.FORWARD);
            backRight.setDirection(DcMotor.Direction.FORWARD);

            // Set zero power behavior to BRAKE
            frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

            // Reset encoders
            resetEncoders();

            telemetry.addData("Status", "Hardware initialized!");
            telemetry.addData("Info", "Press START to begin");
            telemetry.update();

        } catch (Exception e) {
            telemetry.addData("ERROR", "Failed to initialize hardware");
            telemetry.addData("Exception", e.getMessage());
            telemetry.update();
            return;
        }

        waitForStart();

        // Main loop
        while (opModeIsActive()) {

            // Display encoder values continuously if enabled
            if (continuousDisplay) {
                displayEncoderValues();
            }

            // DPAD Controls - Movement Tests
            if (gamepad1.dpad_up) {
                driveDistance(12.0, 0);  // Forward 12 inches
                sleep(500);
            } else if (gamepad1.dpad_down) {
                driveDistance(-12.0, 0);  // Backward 12 inches
                sleep(500);
            } else if (gamepad1.dpad_left) {
                strafeDistance(-12.0);  // Left 12 inches
                sleep(500);
            } else if (gamepad1.dpad_right) {
                strafeDistance(12.0);  // Right 12 inches
                sleep(500);
            }

            // Bumper Controls - Rotation Tests
            if (gamepad1.left_bumper) {
                rotateAngle(90);  // 90° counterclockwise
                sleep(500);
            } else if (gamepad1.right_bumper) {
                rotateAngle(-90);  // 90° clockwise
                sleep(500);
            }

            // Button Controls - Utility
            if (gamepad1.y) {
                resetEncoders();
                telemetry.addData("Action", "Encoders reset to zero");
                telemetry.update();
                sleep(500);
            }

            if (gamepad1.x) {
                continuousDisplay = !continuousDisplay;
                telemetry.addData("Action", "Continuous display: " + continuousDisplay);
                telemetry.update();
                sleep(300);
            }

            if (gamepad1.a) {
                testMovementSequence();
                sleep(500);
            }

            if (gamepad1.b) {
                stopAllMotors();
                telemetry.addData("Action", "All motors stopped");
                telemetry.update();
                sleep(300);
            }

            sleep(50);  // Small delay to prevent CPU overload
        }

        // Stop motors when OpMode ends
        stopAllMotors();
    }

    /**
     * Display current encoder values and calculated position
     */
    private void displayEncoderValues() {
        // Get current encoder positions
        int fl = frontLeft.getCurrentPosition();
        int fr = frontRight.getCurrentPosition();
        int bl = backLeft.getCurrentPosition();
        int br = backRight.getCurrentPosition();

        // Calculate deltas from start
        int flDelta = fl - flStart;
        int frDelta = fr - frStart;
        int blDelta = bl - blStart;
        int brDelta = br - brStart;

        // Calculate distance traveled (average of all wheels)
        double avgTicks = (Math.abs(flDelta) + Math.abs(frDelta) +
                Math.abs(blDelta) + Math.abs(brDelta)) / 4.0;
        double distanceInches = avgTicks / TICKS_PER_INCH;

        // Get IMU heading
        double heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);

        telemetry.addData("=== ENCODER VALUES ===", "");
        telemetry.addData("Front Left", "%d (%d)", fl, flDelta);
        telemetry.addData("Front Right", "%d (%d)", fr, frDelta);
        telemetry.addData("Back Left", "%d (%d)", bl, blDelta);
        telemetry.addData("Back Right", "%d (%d)", br, brDelta);
        telemetry.addData("", "");

        telemetry.addData("=== CALCULATED ===", "");
        telemetry.addData("Average Ticks", "%.1f", avgTicks);
        telemetry.addData("Distance (inches)", "%.2f", distanceInches);
        telemetry.addData("IMU Heading", "%.1f°", heading);
        telemetry.addData("", "");

        telemetry.addData("=== CALIBRATION ===", "");
        telemetry.addData("Ticks per inch", "%.2f", TICKS_PER_INCH);
        telemetry.addData("Ticks per rev", "%.1f", TICKS_PER_REV);
        telemetry.addData("Wheel diameter", "%.3f in", WHEEL_DIAMETER);
        telemetry.addData("", "");

        telemetry.addData("=== CONTROLS ===", "");
        telemetry.addData("DPAD", "Move 12 inches");
        telemetry.addData("BUMPERS", "Rotate 90°");
        telemetry.addData("Y", "Reset encoders");
        telemetry.addData("X", "Toggle display");
        telemetry.addData("A", "Test sequence");
        telemetry.addData("B", "Stop all");

        telemetry.update();
    }

    /**
     * Reset all encoder values to zero
     */
    private void resetEncoders() {
        // Store current positions as start positions
        flStart = frontLeft.getCurrentPosition();
        frStart = frontRight.getCurrentPosition();
        blStart = backLeft.getCurrentPosition();
        brStart = backRight.getCurrentPosition();

        telemetry.addData("Encoders Reset", "Start positions recorded");
        telemetry.update();
    }

    /**
     * Drive forward/backward a specific distance
     * @param inches Distance in inches (positive = forward, negative = backward)
     * @param heading Target heading in degrees
     */
    private void driveDistance(double inches, double heading) {
        telemetry.addData("Action", "Driving %.1f inches", inches);
        telemetry.update();

        // Calculate target encoder ticks
        int targetTicks = (int)(inches * TICKS_PER_INCH);

        // Record starting positions
        int flStart = frontLeft.getCurrentPosition();
        int frStart = frontRight.getCurrentPosition();
        int blStart = backLeft.getCurrentPosition();
        int brStart = backRight.getCurrentPosition();

        // Calculate target positions
        int flTarget = flStart + targetTicks;
        int frTarget = frStart + targetTicks;
        int blTarget = blStart + targetTicks;
        int brTarget = brStart + targetTicks;

        // Set motor powers
        double speed = inches > 0 ? DRIVE_SPEED : -DRIVE_SPEED;
        frontLeft.setPower(speed);
        frontRight.setPower(speed);
        backLeft.setPower(speed);
        backRight.setPower(speed);

        // Wait until target reached
        while (opModeIsActive()) {
            int flCurrent = frontLeft.getCurrentPosition();
            int frCurrent = frontRight.getCurrentPosition();
            int blCurrent = backLeft.getCurrentPosition();
            int brCurrent = backRight.getCurrentPosition();

            int flRemaining = Math.abs(flTarget - flCurrent);
            int frRemaining = Math.abs(frTarget - frCurrent);
            int blRemaining = Math.abs(blTarget - blCurrent);
            int brRemaining = Math.abs(brTarget - brCurrent);

            int avgRemaining = (flRemaining + frRemaining + blRemaining + brRemaining) / 4;

            if (avgRemaining < 50) {  // Within 50 ticks of target
                break;
            }

            telemetry.addData("Target", "%d ticks (%.1f inches)", targetTicks, inches);
            telemetry.addData("Remaining", "%d ticks", avgRemaining);
            telemetry.update();

            sleep(10);
        }

        stopAllMotors();

        telemetry.addData("Complete", "Drove %.1f inches", inches);
        telemetry.update();
    }

    /**
     * Strafe left/right a specific distance
     * @param inches Distance in inches (positive = right, negative = left)
     */
    private void strafeDistance(double inches) {
        telemetry.addData("Action", "Strafing %.1f inches", inches);
        telemetry.update();

        // Calculate target encoder ticks
        int targetTicks = (int)(inches * TICKS_PER_INCH);

        // Record starting positions
        int flStart = frontLeft.getCurrentPosition();
        int frStart = frontRight.getCurrentPosition();
        int blStart = backLeft.getCurrentPosition();
        int brStart = backRight.getCurrentPosition();

        // For strafing: FL and BR move opposite to FR and BL
        int flTarget = flStart + targetTicks;
        int frTarget = frStart - targetTicks;
        int blTarget = blStart - targetTicks;
        int brTarget = brStart + targetTicks;

        // Set motor powers
        double speed = inches > 0 ? DRIVE_SPEED : -DRIVE_SPEED;
        frontLeft.setPower(speed);
        frontRight.setPower(-speed);
        backLeft.setPower(-speed);
        backRight.setPower(speed);

        // Wait until target reached
        while (opModeIsActive()) {
            int flCurrent = frontLeft.getCurrentPosition();
            int frCurrent = frontRight.getCurrentPosition();
            int blCurrent = backLeft.getCurrentPosition();
            int brCurrent = backRight.getCurrentPosition();

            int flRemaining = Math.abs(flTarget - flCurrent);
            int frRemaining = Math.abs(frTarget - frCurrent);
            int blRemaining = Math.abs(blTarget - blCurrent);
            int brRemaining = Math.abs(brTarget - brCurrent);

            int avgRemaining = (flRemaining + frRemaining + blRemaining + brRemaining) / 4;

            if (avgRemaining < 50) {  // Within 50 ticks of target
                break;
            }

            telemetry.addData("Target", "%d ticks (%.1f inches)", targetTicks, inches);
            telemetry.addData("Remaining", "%d ticks", avgRemaining);
            telemetry.update();

            sleep(10);
        }

        stopAllMotors();

        telemetry.addData("Complete", "Strafed %.1f inches", inches);
        telemetry.update();
    }

    /**
     * Rotate by a specific angle using IMU
     * @param degrees Angle in degrees (positive = counterclockwise, negative = clockwise)
     */
    private void rotateAngle(double degrees) {
        telemetry.addData("Action", "Rotating %.1f degrees", degrees);
        telemetry.update();

        double startHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
        double targetHeading = startHeading + degrees;

        // Normalize to -180 to 180
        while (targetHeading > 180) targetHeading -= 360;
        while (targetHeading < -180) targetHeading += 360;

        // Set motor powers for rotation
        double speed = degrees > 0 ? TURN_SPEED : -TURN_SPEED;
        frontLeft.setPower(-speed);
        frontRight.setPower(speed);
        backLeft.setPower(-speed);
        backRight.setPower(speed);

        // Wait until target reached
        while (opModeIsActive()) {
            double currentHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
            double error = targetHeading - currentHeading;

            // Normalize error to -180 to 180
            while (error > 180) error -= 360;
            while (error < -180) error += 360;

            if (Math.abs(error) < 2.0) {  // Within 2 degrees of target
                break;
            }

            telemetry.addData("Target", "%.1f°", targetHeading);
            telemetry.addData("Current", "%.1f°", currentHeading);
            telemetry.addData("Error", "%.1f°", error);
            telemetry.update();

            sleep(10);
        }

        stopAllMotors();

        telemetry.addData("Complete", "Rotated %.1f degrees", degrees);
        telemetry.update();
    }

    /**
     * Test a sequence of movements
     */
    private void testMovementSequence() {
        telemetry.addData("Test Sequence", "Starting...");
        telemetry.update();
        sleep(1000);

        // Square pattern
        driveDistance(12.0, 0);
        sleep(500);

        rotateAngle(90);
        sleep(500);

        driveDistance(12.0, 90);
        sleep(500);

        rotateAngle(90);
        sleep(500);

        driveDistance(12.0, 180);
        sleep(500);

        rotateAngle(90);
        sleep(500);

        driveDistance(12.0, 270);
        sleep(500);

        rotateAngle(90);
        sleep(500);

        telemetry.addData("Test Sequence", "Complete!");
        telemetry.update();
    }

    /**
     * Stop all motors
     */
    private void stopAllMotors() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }
}