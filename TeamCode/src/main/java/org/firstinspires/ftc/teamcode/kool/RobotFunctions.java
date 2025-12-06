package org.firstinspires.ftc.teamcode.kool;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import java.lang.reflect.Method;

/**
 * RobotFunctions - Soft-coded function dispatcher
 * Automatically finds and calls functions based on JSON file names
 * Just create methods with the same names as in your JSON!
 *
 * - "run_while_moving": Function starts at position, robot continues immediately
 * - "wait_till": Robot waits for function to complete before continuing
 */
public class RobotFunctions {

    private HardwareMap hardwareMap;
    private Telemetry telemetry;

    public RobotFunctions(HardwareMap hardwareMap, Telemetry telemetry) {
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;
    }

    /**
     * Dynamically execute a function by name using reflection
     */
    public void executeFunction(String functionName) {
        try {
            Method method = this.getClass().getDeclaredMethod(functionName);
            method.setAccessible(true);
            method.invoke(this);
        } catch (NoSuchMethodException e) {
            telemetry.addData("Warning", "Function '" + functionName + "' not found");
            telemetry.addData("Info", "Create a method: public void " + functionName + "() {...}");
            telemetry.update();
        } catch (Exception e) {
            telemetry.addData("Error", "Failed to call " + functionName + ": " + e.getMessage());
            telemetry.update();
        }
    }

    // ========================================
    // YOUR CUSTOM FUNCTIONS GO BELOW
    // ========================================
    // Just create methods with the same names as in your JSON file
    // No parameters needed - they're called automatically!

    // Example: If JSON has "name": "intake"
    public void intake() {
        telemetry.addData("Function", "Running intake");
        telemetry.update();
        // TODO: Add your intake code here
        // For "run_while_moving": Start motors/servos and return immediately
        // For "wait_till": Complete entire sequence before returning
    }

    // Example: If JSON has "name": "score"
    public void score() {
        telemetry.addData("Function", "Executing score");
        telemetry.update();
        // TODO: Add your score sequence code here
    }

    // Example: If JSON has "name": "park"
    public void park() {
        telemetry.addData("Function", "Executing park");
        telemetry.update();
        // TODO: Add your park sequence code here
    }

    // Add more functions as needed - they will be automatically discovered!
}