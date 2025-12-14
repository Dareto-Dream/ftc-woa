package org.firstinspires.ftc.teamcode.kool;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import java.lang.reflect.Method;

/**
 * RobotFunctions - Soft-coded function dispatcher
 * Automatically finds and calls functions based on AutoData template names
 * Just create methods with the same names as in your AutoData.TEMPLATES array!
 *
 * - "RUN_WHILE_MOVING": Function starts at position, robot continues immediately
 * - "WAIT_TILL": Robot waits for function to complete before continuing
 */
public class RobotFunctions {

    private HardwareMap hardwareMap;
    private Telemetry telemetry;

    // Hardware components
    private DcMotor intake;
    private DcMotor outtake;
    private DcMotor outtake2;

    // State tracking for toggle
    private boolean intakeRunning = false;

    public RobotFunctions(HardwareMap hardwareMap, Telemetry telemetry) {
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;

        // Initialize hardware
        initializeHardware();
    }

    /**
     * Initialize robot hardware components
     */
    private void initializeHardware() {
        try {
            intake = hardwareMap.get(DcMotor.class, "intake");
            outtake = hardwareMap.get(DcMotor.class, "outtake");
            outtake2 = hardwareMap.get(DcMotor.class, "outtake2");

            telemetry.addData("RobotFunctions", "Hardware initialized successfully");
            telemetry.update();
        } catch (Exception e) {
            telemetry.addData("RobotFunctions ERROR", "Failed to initialize hardware");
            telemetry.addData("Exception", e.getMessage());
            telemetry.update();
        }
    }

    /**
     * Dynamically execute a function by name using reflection
     */
    public void executeFunction(String functionName) {
        try {
            Method method = this.getClass().getDeclaredMethod(functionName);
            method.setAccessible(true);
            method.invoke(this);

            telemetry.addData("Function Complete", functionName);
            telemetry.update();

        } catch (NoSuchMethodException e) {
            // Function not found - provide helpful error message
            telemetry.addData("ERROR", "Function '" + functionName + "' not found!");
            telemetry.addData("Info", "Create a method: public void " + functionName + "() {...}");
            telemetry.addData("", ""); // Blank line
            telemetry.addData("Available Templates", "");
            for (String template : AutoData.TEMPLATES) {
                telemetry.addData("  - ", template);
            }
            telemetry.addData("", ""); // Blank line
            telemetry.addData("WARNING", "Continuing without executing this function");
            telemetry.update();

            // Sleep to give time to read the error
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            // Other errors during function execution
            telemetry.addData("ERROR", "Failed to execute " + functionName);
            telemetry.addData("Exception", e.getClass().getSimpleName());
            telemetry.addData("Message", e.getMessage());
            telemetry.update();

            // Sleep to give time to read the error
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ========================================
    // ROBOT-SPECIFIC FUNCTIONS
    // ========================================

    /**
     * INTAKE - Toggle intake motor on/off
     * Each call switches the intake state
     * Perfect for RUN_WHILE_MOVING - starts intake and continues
     */
    public void intake() {
        if (intakeRunning) {
            // Turn OFF
            intake.setPower(0.0);
            intakeRunning = false;

            telemetry.addData("Intake", "OFF");
            telemetry.update();
        } else {
            // Turn ON
            intake.setPower(1.0);
            intakeRunning = true;

            telemetry.addData("Intake", "ON");
            telemetry.update();
        }
    }

    /**
     * SCORE - Run both intake and outtake motors together
     * This runs the complete scoring sequence
     * Best for WAIT_TILL - robot waits for scoring to complete
     */
    public void score() {
        telemetry.addData("Function", "Executing score sequence");
        telemetry.update();

        try {
            // Start all motors for scoring
            // Intake keeps feeding elements
            intake.setPower(1.0);
            // Outtake ejects elements
            outtake.setPower(-1.0);
            outtake2.setPower(-0.99);

            telemetry.addData("Score", "All motors running");
            telemetry.update();

            // Run scoring sequence for set duration
            Thread.sleep(2000); // 2 seconds - adjust as needed

            // Stop all motors
            intake.setPower(0.0);
            outtake.setPower(0.0);
            outtake2.setPower(0.0);

            // Update intake state tracking
            intakeRunning = false;

            telemetry.addData("Score", "Complete - all motors stopped");
            telemetry.update();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Ensure all motors stop on interrupt
            intake.setPower(0.0);
            outtake.setPower(0.0);
            outtake2.setPower(0.0);
            intakeRunning = false;
        } catch (Exception e) {
            telemetry.addData("Score Error", e.getMessage());
            telemetry.update();
            // Ensure all motors stop on error
            intake.setPower(0.0);
            outtake.setPower(0.0);
            outtake2.setPower(0.0);
            intakeRunning = false;
        }
    }

    /**
     * OUTTAKE - Helper function if needed separately
     * Runs only outtake motors (not intake)
     */
    public void outtake() {
        telemetry.addData("Function", "Running outtake only");
        telemetry.update();

        try {
            outtake.setPower(-1.0);
            outtake2.setPower(-0.99);

            Thread.sleep(1000); // Run for 1 second

            outtake.setPower(0.0);
            outtake2.setPower(0.0);

            telemetry.addData("Outtake", "Complete");
            telemetry.update();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            outtake.setPower(0.0);
            outtake2.setPower(0.0);
        } catch (Exception e) {
            telemetry.addData("Outtake Error", e.getMessage());
            telemetry.update();
            outtake.setPower(0.0);
            outtake2.setPower(0.0);
        }
    }

    /**
     * PARK - Prepare robot for parking
     * Ensures all mechanisms are stopped and in safe position
     */
    public void park() {
        telemetry.addData("Function", "Executing park sequence");
        telemetry.update();

        try {
            // Stop all motors
            intake.setPower(0.0);
            outtake.setPower(0.0);
            outtake2.setPower(0.0);

            // Reset state
            intakeRunning = false;

            // If you have servos or arms, retract them here
            // Example: armServo.setPosition(RETRACT_POSITION);

            Thread.sleep(500); // Allow time for mechanisms to settle

            telemetry.addData("Park", "Complete - Ready for parking");
            telemetry.update();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            telemetry.addData("Park Error", e.getMessage());
            telemetry.update();
        }
    }

    // ========================================
    // UTILITY FUNCTIONS
    // ========================================

    /**
     * Stop all motors - emergency stop function
     */
    public void stopAll() {
        intake.setPower(0.0);
        outtake.setPower(0.0);
        outtake2.setPower(0.0);
        intakeRunning = false;

        telemetry.addData("Status", "All motors stopped");
        telemetry.update();
    }

    /**
     * Get current intake state
     */
    public boolean isIntakeRunning() {
        return intakeRunning;
    }

    /**
     * Force intake ON (without toggle)
     */
    public void startIntake() {
        intake.setPower(1.0);
        intakeRunning = true;
        telemetry.addData("Intake", "Started (forced ON)");
        telemetry.update();
    }

    /**
     * Force intake OFF (without toggle)
     */
    public void stopIntake() {
        intake.setPower(0.0);
        intakeRunning = false;
        telemetry.addData("Intake", "Stopped (forced OFF)");
        telemetry.update();
    }
}