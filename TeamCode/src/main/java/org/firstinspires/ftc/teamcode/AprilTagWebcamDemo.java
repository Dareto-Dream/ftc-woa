package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Autonomous(name="AprilTag Webcam Demo", group="Vision")
public class AprilTagWebcamDemo extends LinearOpMode {

    private VisionPortal visionPortal;
    private AprilTagProcessor aprilTag;

    @Override
    public void runOpMode() throws InterruptedException {
        // 1) Create the AprilTag processor (defaults to 36h11, which FTC fields use)
        aprilTag = AprilTagProcessor.easyCreateWithDefaults();

        // 2) Create the VisionPortal attached to your webcam
        visionPortal = VisionPortal.easyCreateWithDefaults(
                hardwareMap.get(WebcamName.class, "Webcam 1"), aprilTag);

        // Optional: tweak camera controls after portal creation (exposure, gain, focus)
        visionPortal.getCameraControl(ExposureControl.class).setMode(ExposureControl.Mode.Manual);
        visionPortal.getCameraControl(ExposureControl.class).setExposure(8, TimeUnit.MILLISECONDS);
        visionPortal.getCameraControl(GainControl.class).setGain(200);

        telemetry.addLine("Init: detecting tags...");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            List<AprilTagDetection> detections = aprilTag.getDetections();
            if (!detections.isEmpty()) {
                AprilTagDetection d = detections.get(0);
                // Pose estimates are in meters and radians
                double x = d.ftcPose.x;      // forward (m)
                double y = d.ftcPose.y;      // left (m)
                double z = d.ftcPose.z;      // up (m)
                double yaw   = d.ftcPose.yaw;
                double pitch = d.ftcPose.pitch;
                double roll  = d.ftcPose.roll;

                telemetry.addData("ID", d.id);
                telemetry.addData("XYZ (m)", "%.3f, %.3f, %.3f", x, y, z);
                telemetry.addData("YPR (deg)", "%.1f, %.1f, %.1f",
                        Math.toDegrees(yaw), Math.toDegrees(pitch), Math.toDegrees(roll));
            } else {
                telemetry.addLine("No tags");
            }
            telemetry.update();
            sleep(20);
        }

        // Clean up
        if (visionPortal != null) {
            visionPortal.close();
        }
    }
}
