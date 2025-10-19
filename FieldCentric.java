package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

@TeleOp(name = "Field Oriented Mecanum (REV SDK 8.2)", group = "TeleOp")
public class FieldCentric extends LinearOpMode {

    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private IMU imu;

    @Override
    public void runOpMode() throws InterruptedException {

        // Map hardware
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");

        imu = hardwareMap.get(IMU.class, "imu");

        // Use RevHubOrientationOnRobot (supported by SDK 8.2)
        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                )
        );
        imu.initialize(parameters);

        telemetry.addLine("Calibrating IMU...");
        telemetry.update();

        telemetry.addLine("IMU Calibrated!");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // Press Y to reset yaw
            if (gamepad1.y) imu.resetYaw();

            double drive  = gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double twist  = gamepad1.right_stick_x;

            // Get current heading
            YawPitchRollAngles orientation = imu.getRobotYawPitchRollAngles();
            double botHeading = orientation.getYaw(AngleUnit.RADIANS);

            // Field-oriented transform:
            // heading (botHeading) is in radians; rotate the driver input by the heading
            double cosA = Math.cos(botHeading);
            double sinA = Math.sin(botHeading);

            // preserve originals while computing rotated values
            double rotatedStrafe = strafe * cosA - drive * sinA;
            double rotatedDrive  = strafe * sinA + drive * cosA;

            // use the rotated values for motor calculations
            strafe = rotatedStrafe;
            drive  = rotatedDrive;

            // Compute motor powers
            double[] speeds = {
                (drive + strafe + twist),
                (drive - strafe - twist),
                (drive - strafe + twist),
                (drive + strafe - twist)
            };

            double max = Math.abs(speeds[0]);
            for(int i = 0; i < speeds.length; i++) {
                if ( max < Math.abs(speeds[i]) ) max = Math.abs(speeds[i]);
            }

            // If and only if the maximum is outside of the range we want it to be,
            // normalize all the other speeds based on the given speed value.
            if (max > 1) {
                for (int i = 0; i < speeds.length; i++) speeds[i] /= max;
            }

            // Apply power
            frontLeft.setPower(speeds[0]);
            frontRight.setPower(speeds[1]);
            backLeft.setPower(speeds[2]);
            backRight.setPower(speeds[3]);

            // Telemetry
            telemetry.addData("Heading (deg)", orientation.getYaw(AngleUnit.DEGREES));
            telemetry.addData("FL", speeds[0]);
            telemetry.addData("FR", speeds[1]);
            telemetry.addData("BL", speeds[2]);
            telemetry.addData("BR", speeds[3]);
            telemetry.update();
        }
    }
}
