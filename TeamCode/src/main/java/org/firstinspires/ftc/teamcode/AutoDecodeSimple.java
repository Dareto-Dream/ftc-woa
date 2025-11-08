package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name="AutoDecode Simple 3-Ball", group="Autonomous")
public class AutoDecodeSimple extends LinearOpMode {

    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private DcMotor intake, outtake, outtake2;
    private VisionPortal visionPortal;
    private AprilTagProcessor aprilTag;

    private int zone = 2; // default = center

    @Override
    public void runOpMode() throws InterruptedException {
        // === Map hardware ===
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");
        intake  = hardwareMap.get(DcMotor.class, "intake");
        outtake = hardwareMap.get(DcMotor.class, "outtake");
        outtake2= hardwareMap.get(DcMotor.class, "outtake2");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);

        // === Vision init ===
        aprilTag = AprilTagProcessor.easyCreateWithDefaults();
        visionPortal = VisionPortal.easyCreateWithDefaults(
                hardwareMap.get(WebcamName.class, "Webcam 1"), aprilTag);

        telemetry.addLine("Detecting AprilTags...");
        telemetry.update();

        // Detect tag before start
        while (!isStarted() && !isStopRequested()) {
            for (AprilTagDetection tag : aprilTag.getDetections()) {
                zone = tag.id; // 1-3 → left/center/right
                telemetry.addData("Tag Detected", tag.id);
            }
            telemetry.update();
            sleep(20);
        }

        waitForStart();
        visionPortal.close();

        // === Autonomous sequence ===
        driveForward(600, 0.4); // leave wall

        // Intake 3 balls
        intake.setPower(1.0);
        driveForward(700, 0.2);
        sleep(2000);
        intake.setPower(0);

        // Shoot balls
        outtake.setPower(1.0);
        outtake2.setPower(0.99);
        sleep(2500); // tune for flywheel speed
        outtake.setPower(0);
        outtake2.setPower(0);

        // Park based on detected zone
        if (zone == 1) strafeLeft(600, 0.4);
        else if (zone == 3) strafeRight(600, 0.4);
        else driveForward(400, 0.3);
    }

    // === Drive helper methods ===
    private void driveForward(int ticks, double power) {
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition(ticks);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            m.setPower(power);
        }
        while (opModeIsActive() && frontLeft.isBusy()) idle();
    }

    private void strafeLeft(int ticks, double power) {
        frontLeft.setTargetPosition(-ticks);
        backLeft.setTargetPosition(ticks);
        frontRight.setTargetPosition(ticks);
        backRight.setTargetPosition(-ticks);
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            m.setPower(power);
        }
        while (opModeIsActive() && frontLeft.isBusy()) idle();
    }

    private void strafeRight(int ticks, double power) {
        strafeLeft(-ticks, power);
    }
}
