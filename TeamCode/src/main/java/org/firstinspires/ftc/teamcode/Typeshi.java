package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Enhanced Mecanum TeleOp with:
 * - Multiple outtake power states
 * - Scoring mode with anti-drift (encoder-based position holding)
 * - Intake toggle and manual modes
 * - Customizable button mapping
 * - Reverse action mappings
 *
 */
@TeleOp(name="Main Code Enhanced", group="Iterative Opmode")
public class Typeshi extends OpMode {

    // ========================================
    // HARDWARE DECLARATIONS
    // ========================================

    // Drive motors
    private DcMotor backRight, backLeft, frontRight, frontLeft = null;

    // Mechanism motors
    private DcMotor intake = null;
    private DcMotor outtake = null;
    private DcMotor outtake2 = null;

    // ========================================
    // CONFIGURATION - CUSTOMIZE HERE
    // ========================================

    // Outtake power states (cycle through with bumpers)
    private static final double[] OUTTAKE_POWERS = {
            0.0,    // State 0: OFF
            -0.25,  // State 1: Low power
            -0.50,  // State 2: Medium power
            -0.75,  // State 3: High power
            -1.00   // State 4: Full power
    };

    // Intake powers
    private static final double INTAKE_POWER = 1.0;
    private static final double INTAKE_REVERSE_POWER = -1.0;

    // Scoring mode settings
    private static final double SCORING_OUTTAKE_POWER = -1.0;
    private static final double SCORING_INTAKE_POWER = 1.0;
    private static final double ANTI_DRIFT_POWER = 0.15;  // Power to resist drift
    private static final int ANTI_DRIFT_THRESHOLD = 50;   // Ticks before correction

    // Drive speed multiplier during scoring mode
    private static final double SCORING_DRIVE_SPEED = 0.3;

    // ========================================
    // BUTTON MAPPING - CUSTOMIZE HERE
    // ========================================

    // Primary gamepad (gamepad1)
    private static final String BTN_INTAKE_TOGGLE = "a";           // Toggle intake on/off
    private static final String BTN_INTAKE_MANUAL = "right_trigger"; // Hold for intake
    private static final String BTN_INTAKE_REVERSE = "left_trigger";  // Hold to reverse intake

    private static final String BTN_OUTTAKE_INCREASE = "right_bumper"; // Increase outtake power
    private static final String BTN_OUTTAKE_DECREASE = "left_bumper";  // Decrease outtake power

    private static final String BTN_SCORING_MODE = "y";            // Toggle scoring mode

    private static final String BTN_EMERGENCY_STOP = "back";       // Stop all mechanisms

    // Secondary actions (if needed)
    private static final String BTN_REVERSE_ALL = "x";             // Reverse all mechanisms temporarily

    // ========================================
    // STATE VARIABLES
    // ========================================

    // Outtake state
    private int currentOuttakeState = 0;  // Index into OUTTAKE_POWERS array
    private boolean lastRightBumper = false;
    private boolean lastLeftBumper = false;

    // Intake state
    private boolean intakeToggleOn = false;
    private boolean lastIntakeToggle = false;

    // Scoring mode state
    private boolean scoringModeActive = false;
    private boolean lastScoringModeButton = false;

    // Anti-drift encoder tracking
    private int flScoringStart = 0;
    private int frScoringStart = 0;
    private int blScoringStart = 0;
    private int brScoringStart = 0;

    // Reverse all state
    private boolean reverseAllActive = false;

    // Emergency stop state
    private boolean emergencyStop = false;

    // ========================================
    // INITIALIZATION
    // ========================================

    @Override
    public void init() {
        // Initialize drive motors
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        // Initialize mechanism motors
        intake = hardwareMap.get(DcMotor.class, "intake");
        outtake = hardwareMap.get(DcMotor.class, "outtake");
        outtake2 = hardwareMap.get(DcMotor.class, "outtake2");

        // Set motor directions
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        // Set brake behavior for precise control
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Outtake States", OUTTAKE_POWERS.length);
        telemetry.addData("Controls", "See driver station for mapping");
        telemetry.update();
    }

    // ========================================
    // MAIN LOOP
    // ========================================

    @Override
    public void loop() {
        // ========================================
        // 1. HANDLE EMERGENCY STOP
        // ========================================
        if (getButtonValue(BTN_EMERGENCY_STOP)) {
            emergencyStop = !emergencyStop;
            if (emergencyStop) {
                stopAllMechanisms();
                telemetry.addData("EMERGENCY", "ALL MECHANISMS STOPPED");
                telemetry.update();
            }
            sleep(200); // Debounce
        }

        if (emergencyStop) {
            // Only allow emergency stop button to toggle it back off
            telemetry.addData("EMERGENCY STOP", "Press BACK to resume");
            telemetry.update();
            return;
        }

        // ========================================
        // 2. HANDLE SCORING MODE TOGGLE
        // ========================================
        boolean scoringButton = getButtonValue(BTN_SCORING_MODE);
        if (scoringButton && !lastScoringModeButton) {
            scoringModeActive = !scoringModeActive;

            if (scoringModeActive) {
                // Record starting encoder positions
                flScoringStart = frontLeft.getCurrentPosition();
                frScoringStart = frontRight.getCurrentPosition();
                blScoringStart = backLeft.getCurrentPosition();
                brScoringStart = backRight.getCurrentPosition();
            }
        }
        lastScoringModeButton = scoringButton;

        // ========================================
        // 3. DRIVE CONTROL
        // ========================================
        double axial = -gamepad1.left_stick_y;
        double lateral = gamepad1.left_stick_x;
        double yaw = gamepad1.right_stick_x;

        // Apply scoring mode speed limit if active
        if (scoringModeActive) {
            axial *= SCORING_DRIVE_SPEED;
            lateral *= SCORING_DRIVE_SPEED;
            yaw *= SCORING_DRIVE_SPEED;

            // Add anti-drift correction
            applyAntiDrift();
        }

        // Calculate wheel powers
        double frontLeftPower = axial + lateral + yaw;
        double frontRightPower = axial - lateral - yaw;
        double backLeftPower = axial - lateral + yaw;
        double backRightPower = axial + lateral - yaw;

        // Normalize wheel powers
        double max = Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower));
        max = Math.max(max, Math.abs(backLeftPower));
        max = Math.max(max, Math.abs(backRightPower));

        if (max > 1.0) {
            frontLeftPower /= max;
            frontRightPower /= max;
            backLeftPower /= max;
            backRightPower /= max;
        }

        // Send power to wheels
        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);

        // ========================================
        // 4. OUTTAKE CONTROL (Power State Cycling)
        // ========================================
        boolean rightBumper = getButtonValue(BTN_OUTTAKE_INCREASE);
        boolean leftBumper = getButtonValue(BTN_OUTTAKE_DECREASE);

        // Increase outtake power state
        if (rightBumper && !lastRightBumper) {
            currentOuttakeState++;
            if (currentOuttakeState >= OUTTAKE_POWERS.length) {
                currentOuttakeState = OUTTAKE_POWERS.length - 1; // Cap at max
            }
        }

        // Decrease outtake power state
        if (leftBumper && !lastLeftBumper) {
            currentOuttakeState--;
            if (currentOuttakeState < 0) {
                currentOuttakeState = 0; // Cap at min (off)
            }
        }

        lastRightBumper = rightBumper;
        lastLeftBumper = leftBumper;

        // Apply outtake power (or scoring mode overrides it)
        if (scoringModeActive) {
            // Scoring mode: full power outtake
            outtake.setPower(SCORING_OUTTAKE_POWER);
            outtake2.setPower(SCORING_OUTTAKE_POWER * 0.99);
        } else if (reverseAllActive) {
            // Reverse mode: reverse the current outtake power
            outtake.setPower(-OUTTAKE_POWERS[currentOuttakeState]);
            outtake2.setPower(-OUTTAKE_POWERS[currentOuttakeState] * 0.99);
        } else {
            // Normal mode: use selected power state
            outtake.setPower(OUTTAKE_POWERS[currentOuttakeState]);
            outtake2.setPower(OUTTAKE_POWERS[currentOuttakeState] * 0.99);
        }

        // ========================================
        // 5. INTAKE CONTROL
        // ========================================
        double intakePower = 0.0;

        // Check reverse all first
        reverseAllActive = getButtonValue(BTN_REVERSE_ALL);

        // Priority 1: Manual intake (trigger held)
        if (getTriggerValue(BTN_INTAKE_MANUAL) > 0.1) {
            intakePower = reverseAllActive ? INTAKE_REVERSE_POWER : INTAKE_POWER;
            intakePower *= getTriggerValue(BTN_INTAKE_MANUAL); // Variable speed
        }
        // Priority 2: Manual reverse (trigger held)
        else if (getTriggerValue(BTN_INTAKE_REVERSE) > 0.1) {
            intakePower = reverseAllActive ? INTAKE_POWER : INTAKE_REVERSE_POWER;
            intakePower *= getTriggerValue(BTN_INTAKE_REVERSE); // Variable speed
        }
        // Priority 3: Scoring mode
        else if (scoringModeActive) {
            intakePower = reverseAllActive ? -SCORING_INTAKE_POWER : SCORING_INTAKE_POWER;
        }
        // Priority 4: Toggle mode
        else {
            // Handle toggle button
            boolean intakeButton = getButtonValue(BTN_INTAKE_TOGGLE);
            if (intakeButton && !lastIntakeToggle) {
                intakeToggleOn = !intakeToggleOn;
            }
            lastIntakeToggle = intakeButton;

            if (intakeToggleOn) {
                intakePower = reverseAllActive ? INTAKE_REVERSE_POWER : INTAKE_POWER;
            }
        }

        intake.setPower(intakePower);

        // ========================================
        // 6. TELEMETRY
        // ========================================
        telemetry.addData("=== DRIVE ===", "");
        telemetry.addData("Front L/R", "%4.2f, %4.2f", frontLeftPower, frontRightPower);
        telemetry.addData("Back  L/R", "%4.2f, %4.2f", backLeftPower, backRightPower);
        telemetry.addData("", "");

        telemetry.addData("=== MECHANISMS ===", "");
        telemetry.addData("Outtake State", "%d/%d (%.2f power)",
                currentOuttakeState, OUTTAKE_POWERS.length - 1, OUTTAKE_POWERS[currentOuttakeState]);
        telemetry.addData("Intake Toggle", intakeToggleOn ? "ON" : "OFF");
        telemetry.addData("Intake Power", "%.2f", intakePower);
        telemetry.addData("", "");

        telemetry.addData("=== MODES ===", "");
        telemetry.addData("Scoring Mode", scoringModeActive ? "ACTIVE" : "inactive");
        telemetry.addData("Reverse All", reverseAllActive ? "ACTIVE" : "inactive");
        telemetry.addData("Emergency Stop", emergencyStop ? "ACTIVE" : "inactive");
        telemetry.addData("", "");

        if (scoringModeActive) {
            int flDrift = Math.abs(frontLeft.getCurrentPosition() - flScoringStart);
            int frDrift = Math.abs(frontRight.getCurrentPosition() - frScoringStart);
            int blDrift = Math.abs(backLeft.getCurrentPosition() - blScoringStart);
            int brDrift = Math.abs(backRight.getCurrentPosition() - brScoringStart);
            int avgDrift = (flDrift + frDrift + blDrift + brDrift) / 4;

            telemetry.addData("Anti-Drift", "Avg: %d ticks", avgDrift);
        }

        telemetry.addData("=== CONTROLS ===", "");
        telemetry.addData("LB/RB", "Outtake -/+");
        telemetry.addData("A", "Intake Toggle");
        telemetry.addData("RT/LT", "Manual Intake/Rev");
        telemetry.addData("Y", "Scoring Mode");
        telemetry.addData("X", "Reverse All");
        telemetry.addData("BACK", "Emergency Stop");

        telemetry.update();
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Apply anti-drift correction during scoring mode
     * Opposes any encoder drift from starting position
     */
    private void applyAntiDrift() {
        // Get current positions
        int flCurrent = frontLeft.getCurrentPosition();
        int frCurrent = frontRight.getCurrentPosition();
        int blCurrent = backLeft.getCurrentPosition();
        int brCurrent = backRight.getCurrentPosition();

        // Calculate drift from scoring start position
        int flDrift = flCurrent - flScoringStart;
        int frDrift = frCurrent - frScoringStart;
        int blDrift = blCurrent - blScoringStart;
        int brDrift = brCurrent - brScoringStart;

        // Apply correction if drift exceeds threshold
        if (Math.abs(flDrift) > ANTI_DRIFT_THRESHOLD ||
                Math.abs(frDrift) > ANTI_DRIFT_THRESHOLD ||
                Math.abs(blDrift) > ANTI_DRIFT_THRESHOLD ||
                Math.abs(brDrift) > ANTI_DRIFT_THRESHOLD) {

            // Calculate correction powers (oppose the drift)
            double flCorrection = -Math.signum(flDrift) * ANTI_DRIFT_POWER;
            double frCorrection = -Math.signum(frDrift) * ANTI_DRIFT_POWER;
            double blCorrection = -Math.signum(blDrift) * ANTI_DRIFT_POWER;
            double brCorrection = -Math.signum(brDrift) * ANTI_DRIFT_POWER;

            // Apply corrections to current motor powers
            frontLeft.setPower(frontLeft.getPower() + flCorrection);
            frontRight.setPower(frontRight.getPower() + frCorrection);
            backLeft.setPower(backLeft.getPower() + blCorrection);
            backRight.setPower(backRight.getPower() + brCorrection);
        }
    }

    /**
     * Stop all mechanism motors
     */
    private void stopAllMechanisms() {
        intake.setPower(0.0);
        outtake.setPower(0.0);
        outtake2.setPower(0.0);
        intakeToggleOn = false;
    }

    /**
     * Get button value from gamepad based on string mapping
     */
    private boolean getButtonValue(String buttonName) {
        switch (buttonName) {
            case "a": return gamepad1.a;
            case "b": return gamepad1.b;
            case "x": return gamepad1.x;
            case "y": return gamepad1.y;
            case "left_bumper": return gamepad1.left_bumper;
            case "right_bumper": return gamepad1.right_bumper;
            case "back": return gamepad1.back;
            case "start": return gamepad1.start;
            case "dpad_up": return gamepad1.dpad_up;
            case "dpad_down": return gamepad1.dpad_down;
            case "dpad_left": return gamepad1.dpad_left;
            case "dpad_right": return gamepad1.dpad_right;
            default: return false;
        }
    }

    /**
     * Get trigger value from gamepad based on string mapping
     */
    private double getTriggerValue(String triggerName) {
        switch (triggerName) {
            case "left_trigger": return gamepad1.left_trigger;
            case "right_trigger": return gamepad1.right_trigger;
            default: return 0.0;
        }
    }

    /**
     * Simple sleep helper
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}