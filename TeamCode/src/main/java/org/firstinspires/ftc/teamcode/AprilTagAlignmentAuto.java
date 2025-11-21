package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

@Autonomous(name="AprilTag Alignment Auto", group="Autonomous")
public class AprilTagAlignmentAuto extends LinearOpMode {

    // ==================== POSITION CONSTANTS (METERS) ====================
    // Adjust these to change where the robot stops relative to the AprilTag
    private static final double TARGET_X = 0.0;    // Left(-)/Right(+) offset from tag (meters)
    private static final double TARGET_Y = 0.0;    // Forward(+)/Back(-) offset from tag (meters)
    private static final double TARGET_Z = 0.4;    // Distance in front of tag (meters) - MAIN DISTANCE

    // ==================== ALIGNMENT TOLERANCES ====================
    private static final double POSITION_TOLERANCE = 0.05;  // How close is "close enough" (meters)
    private static final double ANGLE_TOLERANCE = 5.0;      // Heading tolerance (degrees)

    // ==================== DRIVE CONSTANTS ====================
    private static final double MAX_DRIVE_POWER = 0.3;      // Max speed when approaching
    private static final double MIN_DRIVE_POWER = 0.1;      // Min speed for fine adjustments
    private static final double MAX_TURN_POWER = 0.25;      // Max rotation speed

    // ==================== PID-STYLE GAINS ====================
    private static final double KP_DRIVE = 1.5;    // Forward/back proportional gain
    private static final double KP_STRAFE = 1.5;   // Left/right proportional gain
    private static final double KP_TURN = 0.02;    // Rotation proportional gain

    // ==================== TIMEOUT ====================
    private static final double ALIGNMENT_TIMEOUT = 10.0;  // Max seconds to try aligning

    // Hardware
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private VisionPortal visionPortal;
    private AprilTagProcessor aprilTag;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize hardware
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);

        // Initialize AprilTag detection
        aprilTag = AprilTagProcessor.easyCreateWithDefaults();
        visionPortal = VisionPortal.easyCreateWithDefaults(
                hardwareMap.get(WebcamName.class, "Webcam 1"), aprilTag);

        telemetry.addLine("Ready to scan and align with AprilTag");
        telemetry.addData("Target Distance", "%.2f meters", TARGET_Z);
        telemetry.update();

        waitForStart();

        if (opModeIsActive()) {
            alignToAprilTag();
        }

        // Cleanup
        if (visionPortal != null) {
            visionPortal.close();
        }
    }

    private void alignToAprilTag() {
        double startTime = getRuntime();

        while (opModeIsActive() && (getRuntime() - startTime) < ALIGNMENT_TIMEOUT) {

            AprilTagDetection detection = getAprilTagDetection();

            if (detection == null) {
                telemetry.addLine("❌ No AprilTag detected - searching...");
                telemetry.update();
                stopDrive();
                sleep(100);
                continue;
            }

            // Get current position relative to tag (in meters)
            double currentX = detection.ftcPose.x;      // left(-)/right(+)
            double currentY = detection.ftcPose.y;      // forward(+)/back(-)
            double currentZ = detection.ftcPose.z;      // distance
            double currentYaw = Math.toDegrees(detection.ftcPose.yaw);

            // Calculate errors
            double errorX = TARGET_X - currentX;        // Strafe error
            double errorY = TARGET_Y - currentY;        // Not used (we align based on Z)
            double errorZ = TARGET_Z - currentZ;        // Forward/back error
            double errorYaw = -currentYaw;              // Heading error (negative to face tag)

            // Check if aligned
            if (Math.abs(errorX) < POSITION_TOLERANCE &&
                    Math.abs(errorZ) < POSITION_TOLERANCE &&
                    Math.abs(errorYaw) < ANGLE_TOLERANCE) {

                telemetry.addLine("✅ ALIGNED!");
                telemetry.update();
                stopDrive();
                sleep(500);
                break;
            }

            // Calculate motor powers with proportional control
            double drive = clamp(errorZ * KP_DRIVE, -MAX_DRIVE_POWER, MAX_DRIVE_POWER);
            double strafe = clamp(errorX * KP_STRAFE, -MAX_DRIVE_POWER, MAX_DRIVE_POWER);
            double turn = clamp(errorYaw * KP_TURN, -MAX_TURN_POWER, MAX_TURN_POWER);

            // Apply minimum power threshold for small movements
            if (Math.abs(drive) < MIN_DRIVE_POWER && Math.abs(drive) > 0.01) {
                drive = MIN_DRIVE_POWER * Math.signum(drive);
            }
            if (Math.abs(strafe) < MIN_DRIVE_POWER && Math.abs(strafe) > 0.01) {
                strafe = MIN_DRIVE_POWER * Math.signum(strafe);
            }

            // Set mecanum drive powers
            setMecanumPower(drive, strafe, turn);

            // Telemetry
            telemetry.addLine("🎯 ALIGNING TO TAG #" + detection.id);
            telemetry.addLine("━━━━━━━━━━━━━━━━━━━━━━");
            telemetry.addData("Current Pos", "X:%.3f Y:%.3f Z:%.3f", currentX, currentY, currentZ);
            telemetry.addData("Target Pos", "X:%.3f Y:%.3f Z:%.3f", TARGET_X, TARGET_Y, TARGET_Z);
            telemetry.addLine("━━━━━━━━━━━━━━━━━━━━━━");
            telemetry.addData("Error X (strafe)", "%.3f m", errorX);
            telemetry.addData("Error Z (drive)", "%.3f m", errorZ);
            telemetry.addData("Error Yaw", "%.1f°", errorYaw);
            telemetry.addLine("━━━━━━━━━━━━━━━━━━━━━━");
            telemetry.addData("Drive Power", "%.2f", drive);
            telemetry.addData("Strafe Power", "%.2f", strafe);
            telemetry.addData("Turn Power", "%.2f", turn);
            telemetry.update();

            sleep(20);
        }

        stopDrive();
    }

    private AprilTagDetection getAprilTagDetection() {
        if (aprilTag.getDetections().isEmpty()) {
            return null;
        }
        return aprilTag.getDetections().get(0);
    }

    private void setMecanumPower(double drive, double strafe, double turn) {
        double flPower = drive + strafe + turn;
        double frPower = drive - strafe - turn;
        double blPower = drive - strafe + turn;
        double brPower = drive + strafe - turn;

        // Normalize if any power exceeds 1.0
        double max = Math.max(Math.max(Math.abs(flPower), Math.abs(frPower)),
                Math.max(Math.abs(blPower), Math.abs(brPower)));
        if (max > 1.0) {
            flPower /= max;
            frPower /= max;
            blPower /= max;
            brPower /= max;
        }

        frontLeft.setPower(flPower);
        frontRight.setPower(frPower);
        backLeft.setPower(blPower);
        backRight.setPower(brPower);
    }

    private void stopDrive() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}