package org.firstinspires.ftc.teamcode.kool;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@Autonomous(name = "Auto Path Follower", group = "Autonomous")
public class AutoPathFollower extends LinearOpMode {

    // Hardware
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private IMU imu;

    // Constants
    private static final double COUNTS_PER_MOTOR_REV = 384.5;  // Encoder ticks per output shaft revolution
    private static final double WHEEL_DIAMETER_INCHES = 4.094;
    private static final double COUNTS_PER_INCH =
            COUNTS_PER_MOTOR_REV / (WHEEL_DIAMETER_INCHES * Math.PI);

    private static final double DRIVE_SPEED = 0.6;
    private static final double POSITION_TOLERANCE = 2.0; // inches
    private static final long MOVEMENT_TIMEOUT_MS = 10000; // 10 second timeout
    private static final long ROTATION_TIMEOUT_MS = 5000; // 5 second timeout

    // Path data - now from AutoData class
    private double currentX, currentY, currentRotation;
    private double startingRotationOffset; // To account for initial robot orientation
    private boolean useEncoders = false;

    // Function interfaces - to be implemented by user
    private RobotFunctions robotFunctions;

    // Thread synchronization for telemetry
    private final Object telemetryLock = new Object();
    @Override
    public void runOpMode() {
        // Initialize hardware
        initializeHardware();

        // Initialize robot functions
        robotFunctions = new RobotFunctions(hardwareMap, telemetry);

        // Set starting position from AutoData
        currentX = AutoData.START_POS.x;
        currentY = AutoData.START_POS.y;
        currentRotation = AutoData.START_POS.rotation;
        startingRotationOffset = Math.toRadians(AutoData.START_POS.rotation);

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Starting Position", "X: %.1f, Y: %.1f, Rot: %.1f deg",
                currentX, currentY, currentRotation);
        telemetry.addData("Path Points", AutoData.PATH.length);
        telemetry.addData("Functions", AutoData.FUNCTIONS.length);
        telemetry.addData("Using Encoders", useEncoders);
        telemetry.update();

        waitForStart();

        if (opModeIsActive()) {
            executePath();
        }
    }

    private void initializeHardware() {
        // Initialize motors
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        // Set motor directions for mecanum drive
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        // Set zero power behavior to BRAKE for better precision
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Check if encoders are available
        try {
            frontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            frontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            backLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            backRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            useEncoders = true;
            telemetry.addData("Encoders", "Available and active");
        } catch (Exception e) {
            telemetry.addData("Warning", "Encoders not available, using time-based movement");
            useEncoders = false;

            frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            backLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            backRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        // Initialize IMU for field-centric drive
        imu = hardwareMap.get(IMU.class, "imu");
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD));
        imu.initialize(parameters);
        imu.resetYaw();

        telemetry.addData("IMU", "Initialized (yaw reset to 0)");
        telemetry.addData("Note", "Robot should be facing field 0 deg or set START_POS.rotation accordingly");
    }

    private void executePath() {
        AutoData.Point[] path = AutoData.PATH;
        AutoData.FunctionData[] functions = AutoData.FUNCTIONS;

        for (int i = 0; i < path.length; i++) {
            if (!opModeIsActive()) break;

            AutoData.Point waypoint = path[i];
            double targetX = waypoint.x;
            double targetY = waypoint.y;

            synchronized (telemetryLock) {
                telemetry.addData("Waypoint", "%d of %d", i + 1, path.length);
                telemetry.addData("Target", "X: %.1f, Y: %.1f", targetX, targetY);
                telemetry.update();
            }

            // Move to position
            moveToPosition(targetX, targetY);
            currentX = targetX;
            currentY = targetY;

            // Check if there's a function at this position
            AutoData.FunctionData functionAtWaypoint = getFunctionAtPosition(functions, targetX, targetY);

            if (functionAtWaypoint != null) {
                AutoData.FunctionType actionType = functionAtWaypoint.type;
                String functionName = functionAtWaypoint.name;
                double targetRotation = functionAtWaypoint.rotation;

                if (actionType == AutoData.FunctionType.RUN_WHILE_MOVING) {
                    // Start function in background thread - robot continues immediately
                    startFunctionInBackground(functionName);
                    synchronized (telemetryLock) {
                        telemetry.addData("Function", functionName + " started (background)");
                        telemetry.update();
                    }

                } else if (actionType == AutoData.FunctionType.WAIT_TILL) {
                    // Rotate to target angle first
                    rotateToAngle(targetRotation);

                    // Execute function and wait for completion
                    executeFunction(functionName);
                    synchronized (telemetryLock) {
                        telemetry.addData("Function", functionName + " completed");
                        telemetry.update();
                    }
                }
            }
        }

        synchronized (telemetryLock) {
            telemetry.addData("Status", "Path complete!");
            telemetry.update();
        }
    }

    private AutoData.FunctionData getFunctionAtPosition(AutoData.FunctionData[] functions,
                                                        double targetX, double targetY) {
        for (AutoData.FunctionData func : functions) {
            if (Math.abs(func.x - targetX) < POSITION_TOLERANCE &&
                    Math.abs(func.y - targetY) < POSITION_TOLERANCE) {
                return func;
            }
        }
        return null;
    }

    private void moveToPosition(double targetX, double targetY) {
        double deltaX = targetX - currentX;
        double deltaY = targetY - currentY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance < POSITION_TOLERANCE) {
            return; // Already at target
        }

        if (useEncoders) {
            moveWithEncoders(deltaX, deltaY, distance);
        } else {
            moveWithTime(deltaX, deltaY, distance);
        }
    }

    private void moveWithEncoders(double deltaX, double deltaY, double distance) {
        // Get robot heading from IMU (accounting for starting offset)
        double robotHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS) + startingRotationOffset;

        // Convert field-centric to robot-centric
        double fieldAngle = Math.atan2(deltaY, deltaX);
        double robotAngle = fieldAngle - robotHeading;

        // Calculate mecanum drive vectors
        double x = Math.cos(robotAngle);
        double y = Math.sin(robotAngle);

        // Mecanum drive equations (field-centric)
        double frontLeftPower = y + x;
        double frontRightPower = y - x;
        double backLeftPower = y - x;
        double backRightPower = y + x;

        // Normalize powers
        double maxPower = Math.max(Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower)));
        if (maxPower > 1.0) {
            frontLeftPower /= maxPower;
            frontRightPower /= maxPower;
            backLeftPower /= maxPower;
            backRightPower /= maxPower;
        }

        // Calculate target encoder counts
        int targetCounts = (int) (distance * COUNTS_PER_INCH);

        // Reset encoders
        int startFL = frontLeft.getCurrentPosition();
        int startFR = frontRight.getCurrentPosition();
        int startBL = backLeft.getCurrentPosition();
        int startBR = backRight.getCurrentPosition();

        // Set target positions
        frontLeft.setTargetPosition(startFL + (int)(targetCounts * frontLeftPower));
        frontRight.setTargetPosition(startFR + (int)(targetCounts * frontRightPower));
        backLeft.setTargetPosition(startBL + (int)(targetCounts * backLeftPower));
        backRight.setTargetPosition(startBR + (int)(targetCounts * backRightPower));

        // Set to RUN_TO_POSITION mode
        frontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // Set power
        frontLeft.setPower(Math.abs(DRIVE_SPEED * frontLeftPower));
        frontRight.setPower(Math.abs(DRIVE_SPEED * frontRightPower));
        backLeft.setPower(Math.abs(DRIVE_SPEED * backLeftPower));
        backRight.setPower(Math.abs(DRIVE_SPEED * backRightPower));

        // Wait until motors reach target (with timeout)
        long startTime = System.currentTimeMillis();
        while (opModeIsActive() &&
                (frontLeft.isBusy() || frontRight.isBusy() || backLeft.isBusy() || backRight.isBusy())) {

            // Check for timeout
            if (System.currentTimeMillis() - startTime > MOVEMENT_TIMEOUT_MS) {
                synchronized (telemetryLock) {
                    telemetry.addData("Warning", "Movement timeout reached");
                    telemetry.update();
                }
                break;
            }

            synchronized (telemetryLock) {
                double avgCurrent = getAverageEncoderPosition();
                double avgStart = getAverageStartPosition(startFL, startFR, startBL, startBR);
                double traveled = (avgCurrent - avgStart) / COUNTS_PER_INCH;
                telemetry.addData("Distance Remaining", "%.2f inches", distance - traveled);
                telemetry.update();
            }
        }

        // Stop motors
        stopMotors();

        // Return to RUN_USING_ENCODER mode
        frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void moveWithTime(double deltaX, double deltaY, double distance) {
        // Get robot heading from IMU (accounting for starting offset)
        double robotHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS) + startingRotationOffset;

        // Convert field-centric to robot-centric
        double fieldAngle = Math.atan2(deltaY, deltaX);
        double robotAngle = fieldAngle - robotHeading;

        // Calculate mecanum drive vectors
        double x = Math.cos(robotAngle);
        double y = Math.sin(robotAngle);

        // Mecanum drive equations
        double frontLeftPower = y + x;
        double frontRightPower = y - x;
        double backLeftPower = y - x;
        double backRightPower = y + x;

        // Normalize powers
        double maxPower = Math.max(Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower)));
        if (maxPower > 1.0) {
            frontLeftPower /= maxPower;
            frontRightPower /= maxPower;
            backLeftPower /= maxPower;
            backRightPower /= maxPower;
        }

        // Estimate time based on distance
        // This needs calibration for your specific robot!
        // Current estimate assumes ~12 inches per second at DRIVE_SPEED=0.6
        double estimatedTime = (distance / 12.0) * 1000; // milliseconds

        // Set motor powers
        frontLeft.setPower(DRIVE_SPEED * frontLeftPower);
        frontRight.setPower(DRIVE_SPEED * frontRightPower);
        backLeft.setPower(DRIVE_SPEED * backLeftPower);
        backRight.setPower(DRIVE_SPEED * backRightPower);

        sleep((long) estimatedTime);

        stopMotors();
    }

    private void rotateToAngle(double targetAngleDegrees) {
        double targetAngleRadians = Math.toRadians(targetAngleDegrees);
        double currentAngle = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS) + startingRotationOffset;
        double angleDiff = normalizeAngle(targetAngleRadians - currentAngle);

        double rotationPower = 0.3;
        long startTime = System.currentTimeMillis();

        while (opModeIsActive() && Math.abs(angleDiff) > Math.toRadians(2)) {

            // Check for timeout
            if (System.currentTimeMillis() - startTime > ROTATION_TIMEOUT_MS) {
                synchronized (telemetryLock) {
                    telemetry.addData("Warning", "Rotation timeout reached");
                    telemetry.update();
                }
                break;
            }

            double power = Math.signum(angleDiff) * rotationPower;

            frontLeft.setPower(-power);
            frontRight.setPower(power);
            backLeft.setPower(-power);
            backRight.setPower(power);

            currentAngle = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS) + startingRotationOffset;
            angleDiff = normalizeAngle(targetAngleRadians - currentAngle);

            synchronized (telemetryLock) {
                telemetry.addData("Rotating", "Target: %.1f deg, Current: %.1f deg",
                        Math.toDegrees(targetAngleRadians), Math.toDegrees(currentAngle));
                telemetry.update();
            }
        }

        stopMotors();
        currentRotation = targetAngleDegrees;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private void stopMotors() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }

    private double getAverageEncoderPosition() {
        return (Math.abs(frontLeft.getCurrentPosition()) +
                Math.abs(frontRight.getCurrentPosition()) +
                Math.abs(backLeft.getCurrentPosition()) +
                Math.abs(backRight.getCurrentPosition())) / 4.0;
    }

    private double getAverageStartPosition(int startFL, int startFR, int startBL, int startBR) {
        // FIXED: Removed Math.abs() to correctly calculate average start position
        return (startFL + startFR + startBL + startBR) / 4.0;
    }

    // Function execution methods - delegated to RobotFunctions class
    private void startFunctionInBackground(String functionName) {
        // Run function in separate thread so robot can continue moving
        new Thread(() -> {
            synchronized (telemetryLock) {
                telemetry.addData("Background Function", functionName + " starting");
                telemetry.update();
            }
            robotFunctions.executeFunction(functionName);
            synchronized (telemetryLock) {
                telemetry.addData("Background Function", functionName + " completed");
                telemetry.update();
            }
        }).start();
    }

    private void executeFunction(String functionName) {
        synchronized (telemetryLock) {
            telemetry.addData("Executing Function", functionName);
            telemetry.update();
        }
        robotFunctions.executeFunction(functionName);
    }
}