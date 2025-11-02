package org.firstinspires.ftc.teamcode;
// import lines were omitted. OnBotJava will add them automatically.

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@com.qualcomm.robotcore.eventloop.opmode.Autonomous
public class Autonomous extends LinearOpMode {
    // Use four drive motors (DcMotorEx) so we can control them together
    private DcMotorEx frontLeft, frontRight, backLeft, backRight;

    @Override
    public void runOpMode() {
        // Map hardware - use underscore names from robot configuration
        frontLeft  = hardwareMap.get(DcMotorEx.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotorEx.class, "backLeft");
        backRight  = hardwareMap.get(DcMotorEx.class, "backRight");

        // Reset encoders for all motors during initialization
        frontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        waitForStart();

        // Set target position (ticks) for each motor
        final int targetTicks = 300;
        frontLeft.setTargetPosition(targetTicks);
        frontRight.setTargetPosition(targetTicks);
        backLeft.setTargetPosition(targetTicks);
        backRight.setTargetPosition(targetTicks);

        // Switch to RUN_TO_POSITION mode for all motors
        frontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // Start the motors moving by setting the max velocity (ticks per second)
        final double velocity = 200.0;
        frontLeft.setVelocity(velocity);
        frontRight.setVelocity(velocity);
        backLeft.setVelocity(velocity);
        backRight.setVelocity(velocity);

        // While the Op Mode is running, show each motor's status via telemetry
        while (opModeIsActive()) {
            telemetry.addData("FL pos", frontLeft.getCurrentPosition());
            telemetry.addData("FL vel", frontLeft.getVelocity());
            telemetry.addData("FL at target", !frontLeft.isBusy());

            telemetry.addData("FR pos", frontRight.getCurrentPosition());
            telemetry.addData("FR vel", frontRight.getVelocity());
            telemetry.addData("FR at target", !frontRight.isBusy());

            telemetry.addData("BL pos", backLeft.getCurrentPosition());
            telemetry.addData("BL vel", backLeft.getVelocity());
            telemetry.addData("BL at target", !backLeft.isBusy());

            telemetry.addData("BR pos", backRight.getCurrentPosition());
            telemetry.addData("BR vel", backRight.getVelocity());
            telemetry.addData("BR at target", !backRight.isBusy());

            telemetry.update();
        }
    }
}