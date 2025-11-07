package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name="Main Code Field-Centric", group="Iterative Opmode")
public class Typeshi_FieldCentric extends OpMode {

    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private DcMotor intake, outtake, outtake2;
    private BNO055IMU imu;

    @Override
    public void init() {
        // Drive motors
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");

        intake  = hardwareMap.get(DcMotor.class, "intake");
        outtake = hardwareMap.get(DcMotor.class, "outtake");
        outtake2= hardwareMap.get(DcMotor.class, "outtake2");

        // Reverse left side
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);

        // Setup IMU
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters params = new BNO055IMU.Parameters();
        params.angleUnit = BNO055IMU.AngleUnit.RADIANS;
        imu.initialize(params);

        telemetry.addData("Status", "Initialized + IMU Calibrating");
    }

    @Override
    public void loop() {
        // === Inputs ===
        double axial   = -gamepad1.left_stick_y; // forward/back
        double lateral = gamepad1.left_stick_x;  // strafe
        double yaw     = gamepad1.right_stick_x; // rotation
        boolean intakeOn  = gamepad1.a;
        boolean outtakeOn = gamepad1.b;

        // === Get Robot Heading ===
        double heading = imu.getAngularOrientation().firstAngle; // radians

        // === Apply Field-Centric Rotation ===
        double rotX = lateral * Math.cos(heading) - axial * Math.sin(heading);
        double rotY = lateral * Math.sin(heading) + axial * Math.cos(heading);

        // === Calculate Mecanum Power ===
        double frontLeftPower  = rotY + rotX + yaw;
        double frontRightPower = rotY - rotX - yaw;
        double backLeftPower   = rotY - rotX + yaw;
        double backRightPower  = rotY + rotX - yaw;

        // === Normalize ===
        double max = Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower));
        max = Math.max(max, Math.abs(backLeftPower));
        max = Math.max(max, Math.abs(backRightPower));
        if (max > 1.0) {
            frontLeftPower  /= max;
            frontRightPower /= max;
            backLeftPower   /= max;
            backRightPower  /= max;
        }

        // === Set Power ===
        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);

        // === Intake / Outtake ===
        intake.setPower(intakeOn ? 1.0 : 0.0);
        if (outtakeOn) {
            outtake.setPower(1.0);
            outtake2.setPower(0.99);
        } else {
            outtake.setPower(0.0);
            outtake2.setPower(0.0);
        }

        // === Telemetry ===
        telemetry.addData("Heading (deg)", Math.toDegrees(heading));
        telemetry.addData("Front L/R", "%4.2f | %4.2f", frontLeftPower, frontRightPower);
        telemetry.addData("Back  L/R", "%4.2f | %4.2f", backLeftPower, backRightPower);
        telemetry.update();
    }
}
