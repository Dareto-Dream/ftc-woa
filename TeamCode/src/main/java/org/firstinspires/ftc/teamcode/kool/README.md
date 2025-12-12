# FTC 2025 Field-Centric Autonomous Path Follower
## AutoData.java-Based Navigation System

**Version 3.0** - AutoData.java replaces runtime JSON parsing

---

## Table of Contents
- [Overview](#overview)
- [Changelog](#changelog)
- [Features](#features)
- [Quick Start](#quick-start)
- [File Structure](#file-structure)
- [Understanding the System](#understanding-the-system)
- [AutoData Format](#autodata-format)
- [Creating Custom Functions](#creating-custom-functions)
- [Configuration](#configuration)
- [Advanced Usage](#advanced-usage)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)

---

## Overview

This autonomous system allows you to define robot paths and actions using JSON files that are converted to Java code (AutoData.java). The robot follows waypoints while executing custom functions either in the background (while moving) or blocking (waiting for completion).

### Key Capabilities:
- Field-centric mecanum drive with IMU
- Encoder-based movement verification (auto-fallback to time-based)
- Background threading for run-while-moving functions
- True soft-coding - functions discovered automatically by name
- JSON-to-Java conversion for compile-time safety
- Three function execution modes

---

## Changelog

### Version 3.0 (Current)
**Major Update - AutoData.java Replaces Runtime JSON**

#### Breaking Changes:
- NEW: Python converter generates AutoData.java from JSON files
    - JSON files no longer loaded at runtime
    - Compile-time type safety and error checking
    - Faster startup - no JSON parsing overhead
- NEW: Removed all JSON parsing dependencies
    - No org.json imports needed
    - No asset folder required
    - Simpler deployment
- CHANGED: Workflow now includes conversion step
    - Edit JSON → Run converter → Build project

#### Improvements:
- Type-safe access to all path and function data
- Eliminates runtime JSON parsing errors
- Better IDE support with autocomplete
- Smaller APK size (no JSON library needed)
- Clearer error messages at compile time

### Version 2.0
**Major Rewrite - Background Threading & True Soft-Coding**

#### Breaking Changes:
- NEW: `run_while_moving` functions now execute in background threads
    - Functions can include sleep() and delays without blocking robot movement
    - Robot continues to next waypoint while function executes
- NEW: True soft-coded function dispatch using Java reflection
    - No more switch statements or if/else blocks
    - Just create methods matching template names
- CHANGED: Removed `startFunction()` and `stopFunction()` methods
    - All functions use single `executeFunction()` call
    - Background vs blocking determined by type, not function design

#### Improvements:
- Simplified RobotFunctions class
- Better error handling with Exception catching
- Clearer execution flow: Move → Check position → Execute function
- Updated package to `org.firstinspires.ftc.teamcode.kool`
- Comprehensive documentation

### Version 1.0 (Initial)
- Basic path following with JSON
- Hard-coded switch statements for functions
- Sequential execution only
- Manual function start/stop

---

## Features

### Navigation
- **Field-Centric Drive**: Robot maintains orientation relative to field, not its heading
- **Encoder Verification**: Uses motor encoders (145.1 PPR @ output shaft) for precise distance
- **Auto-Fallback**: Automatically switches to time-based movement if encoders unavailable
- **4-Wheel Mecanum**: Optimized for mecanum drive kinematics

### Function Execution
- **Background Threading**: Functions run asynchronously while robot continues moving
- **Blocking Execution**: Robot waits for critical functions to complete
- **Rotation Control**: Automatic rotation to specified angles
- **Soft-Coded Discovery**: Functions automatically discovered by name using reflection

### Configuration
- **JSON-to-Java Workflow**: Edit JSON, convert to Java, then compile
- **Type Safety**: All data validated at compile time
- **Hot-Swappable**: Change JSON, regenerate AutoData.java, rebuild
- **No Runtime Overhead**: All data compiled directly into APK

---

## Quick Start

### 1. Setup File Structure

```
TeamCode/
├── src/
│   └── main/
│       └── java/
│           └── org/firstinspires/ftc/teamcode/kool/
│               ├── AutoPathFollower.java      ← PUT HERE
│               ├── RobotFunctions.java        ← PUT HERE
│               └── AutoData.java              ← GENERATED FILE
└── (workspace root)
    ├── functions.json                         ← CREATE HERE
    ├── path.json                              ← CREATE HERE
    └── json_to_java_converter.py              ← PUT HERE
```

### 2. Create JSON Files
Create `functions.json` and `path.json` in your workspace root (or any convenient location)

### 3. Generate AutoData.java
Run the Python converter:
```bash
python3 json_to_java_converter.py
```

This generates `AutoData.java` in the same directory.

### 4. Copy AutoData.java
Move `AutoData.java` to `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/kool/`

### 5. Add Java Files
Copy `AutoPathFollower.java` and `RobotFunctions.java` to the same package

### 6. Build & Deploy
1. Click `Build` → `Make Project`
2. Connect to Robot Controller
3. Click Run

---

## File Structure

### Required Files

| File | Location | Purpose |
|------|----------|---------|
| `AutoPathFollower.java` | `teamcode/kool/` | Main autonomous OpMode |
| `RobotFunctions.java` | `teamcode/kool/` | Your custom functions |
| `AutoData.java` | `teamcode/kool/` | Generated data class |
| `functions.json` | Workspace | Source for function data |
| `path.json` | Workspace | Source for path data |
| `json_to_java_converter.py` | Workspace | Converter script |

### Workflow Files

| File | Generated | Committed to Git |
|------|-----------|------------------|
| `functions.json` | No (manual) | Yes |
| `path.json` | No (manual) | Yes |
| `AutoData.java` | Yes (from converter) | Optional |
| `json_to_java_converter.py` | No (provided) | Yes |

**Recommendation**: Commit JSON files to Git. Optionally commit AutoData.java or regenerate it as part of your build process.

---

## Understanding the System

### Core Concept: Functions are Markers

**Important**: Function coordinates do NOT define where the robot moves!

- **`path.json`** defines WHERE the robot moves (waypoints)
- **`functions.json`** defines WHAT to do when robot is at specific positions (markers)
- **`AutoData.java`** contains the compiled data from both files

### Execution Flow

```
1. Load data from AutoData class (compile-time)
2. Set starting position from AutoData.START_POS
3. For each waypoint in AutoData.PATH:
   a. Move to waypoint coordinates
   b. Update current position
   c. Check: Is there a function at this position?
   d. If yes:
      - RUN_WHILE_MOVING → Start in background, continue immediately
      - WAIT_TILL + FUNCTION → Execute and wait for completion
   e. Continue to next waypoint
4. Path complete
```

### The Two Function Types

#### Type 1: RUN_WHILE_MOVING
Robot arrives → Start function in background thread → Continue moving immediately

**Use for**: Starting intake/outtake while driving

**Example JSON**:
```json
{"name": "intake", "x": 36, "y": 84, "type": "run_while_moving", "action": "function"}
```

**Java Method**:
```java
public void intake() {
    intakeMotor.setPower(0.8);
    sleep(2000);  // Robot keeps moving during this!
    intakeMotor.setPower(0);
}
```

**Generated in AutoData.java**:
```java
new FunctionData(
    "intake",
    36,
    84,
    0,
    FunctionType.RUN_WHILE_MOVING,
    ActionType.FUNCTION
)
```

#### Type 2: WAIT_TILL
Robot arrives → Execute function → Wait for completion → Continue

**Use for**: Scoring, complex sequences, rotations

**Example JSON**:
```json
{"name": "score", "x": 96, "y": 24, "rotation": 315, "type": "wait_till", "action": "function"}
```

**Java Method**:
```java
public void score() {
    armMotor.setTargetPosition(1000);
    sleep(1000);  // Robot waits here
    clawServo.setPosition(0.5);
}
```

**Generated in AutoData.java**:
```java
new FunctionData(
    "score",
    96,
    24,
    315,
    FunctionType.WAIT_TILL,
    ActionType.FUNCTION
)
```

---

## AutoData Format

### Generated AutoData.java Structure

```java
public class AutoData {
    
    // Path points
    public static final Point[] PATH = {
        new Point(120, 16),
        new Point(84, 52),
        // ... more points
    };
    
    // Start position
    public static final Position START_POS = new Position(
        120,
        16,
        135
    );
    
    // Functions
    public static final FunctionData[] FUNCTIONS = {
        new FunctionData(
            "intake",
            104,
            60,
            0,
            FunctionType.RUN_WHILE_MOVING,
            ActionType.FUNCTION
        ),
        // ... more functions
    };
    
    // Templates
    public static final String[] TEMPLATES = { "intake", "outtake", "score", "park" };
    
    // Helper classes and enums...
}
```

### Source JSON Files

**path.json**:
```json
{
  "path": [
    {"x": 120, "y": 16},
    {"x": 84, "y": 52},
    {"x": 104, "y": 60}
  ]
}
```

**functions.json**:
```json
{
  "functions": [
    {
      "name": "intake",
      "x": 104,
      "y": 60,
      "rotation": 0,
      "type": "run_while_moving",
      "action": "function"
    }
  ],
  "templates": [
    "intake",
    "outtake",
    "score",
    "park"
  ],
  "start_pos": {
    "x": 120,
    "y": 16,
    "rotation": 135
  }
}
```

**JSON Parameters**:
- `name`: Must match Java method name exactly
- `x`, `y`: Must match a waypoint in path.json
- `rotation`: Target angle in degrees
- `type`: `"run_while_moving"` or `"wait_till"`
- `action`: `"function"` (currently only supported type)
- `templates`: Array of function names that should exist in RobotFunctions

**Critical**: Function coordinates must match waypoints within POSITION_TOLERANCE (default 2.0 inches)!

---

## Creating Custom Functions

### Step 1: Add to functions.json
```json
{
  "functions": [
    {"name": "my_function", "x": 48, "y": 96, "rotation": 0, "type": "wait_till", "action": "function"}
  ],
  "templates": ["my_function", "intake", "score"]
}
```

### Step 2: Regenerate AutoData.java
```bash
python3 json_to_java_converter.py
```

### Step 3: Copy to Project
Move the new `AutoData.java` to your project's `kool` package

### Step 4: Create Method in RobotFunctions.java
```java
public void my_function() {
    // Your code here
    clawServo.setPosition(0.5);
    sleep(300);
}
```

### Step 5: Build & Deploy
That's it! Auto-discovered by name - no switch statements needed!

### Guidelines

**For RUN_WHILE_MOVING**:
```java
public void start_intake() {
    intakeMotor.setPower(0.8);
    sleep(2000);  // OK! Runs in background
    intakeMotor.setPower(0);
}
```

**For WAIT_TILL**:
```java
public void score() {
    armMotor.setTargetPosition(2000);
    while (armMotor.isBusy()) sleep(10);  // Robot waits
    clawServo.setPosition(0.5);
}
```

---

## Configuration

### Hardware Names
Update in `AutoPathFollower.java`:
```java
frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
frontRight = hardwareMap.get(DcMotor.class, "frontRight");
backLeft = hardwareMap.get(DcMotor.class, "backLeft");
backRight = hardwareMap.get(DcMotor.class, "backRight");
imu = hardwareMap.get(IMU.class, "imu");
```

### Constants
Update in `AutoPathFollower.java`:
```java
COUNTS_PER_MOTOR_REV = 145.1;    // Adjust for your motors
WHEEL_DIAMETER_INCHES = 4.0;     // Measure your wheels
DRIVE_SPEED = 0.6;               // Tune for accuracy
POSITION_TOLERANCE = 2.0;        // Arrival threshold
```

---

## Advanced Usage

### Modifying the Converter

The Python converter can be customized for different naming conventions or data structures. Edit `json_to_java_converter.py` to:

- Change the package name
- Modify class structure
- Add custom data fields
- Change enum names

### Automated Build Integration

For advanced workflows, integrate the converter into your build process:

**Option 1: Pre-build script**
```bash
#!/bin/bash
python3 json_to_java_converter.py
cp AutoData.java TeamCode/src/main/java/org/firstinspires/ftc/teamcode/kool/
```

**Option 2: Gradle task**
Add to your `build.gradle`:
```gradle
task generateAutoData(type: Exec) {
    commandLine 'python3', 'json_to_java_converter.py'
}

preBuild.dependsOn generateAutoData
```

### Version Control Best Practices

**Commit to Git**:
- functions.json
- path.json
- json_to_java_converter.py
- AutoPathFollower.java
- RobotFunctions.java

**Optional (Git Ignore or Commit)**:
- AutoData.java

If you gitignore AutoData.java, add a note in your README to run the converter after cloning.

---

## Troubleshooting

### Functions Not Executing
1. Verify coordinates match exactly (path vs functions)
2. Check function name matches method name (case-sensitive)
3. Look for "Function not found" in telemetry
4. Verify AutoData.java is in the correct package
5. Ensure you regenerated AutoData.java after changing JSON

### Compilation Errors
1. Check AutoData.java is in `org.firstinspires.ftc.teamcode.kool` package
2. Verify JSON syntax is valid (use jsonlint.com)
3. Regenerate AutoData.java with latest converter
4. Clean & Rebuild Project

### Robot Not Moving
1. Check motor directions in initializeHardware()
2. Verify encoder connections
3. Adjust `COUNTS_PER_MOTOR_REV`
4. Check wheel diameter measurement
5. Review telemetry output for errors

### Converter Errors
1. Ensure functions.json and path.json are in the same directory as converter
2. Validate JSON syntax at jsonlint.com
3. Check for proper quotes (must be double quotes)
4. Verify all required fields are present

### Template Warnings
If you see "Function 'X' not found" warnings:
1. Check that the function name in JSON matches the method name exactly
2. Verify the method is public and takes no parameters
3. Ensure the method is in RobotFunctions.java
4. Check AutoData.TEMPLATES array contains the function name

---

## Examples

### Example 1: Simple Path with Intake

**path.json**:
```json
{
  "path": [
    {"x": 12, "y": 12},
    {"x": 36, "y": 12},
    {"x": 36, "y": 96}
  ]
}
```

**functions.json**:
```json
{
  "functions": [
    {"name": "intake", "x": 36, "y": 12, "rotation": 0, "type": "wait_till", "action": "function"},
    {"name": "score", "x": 36, "y": 96, "rotation": 90, "type": "wait_till", "action": "function"}
  ],
  "templates": ["intake", "score"],
  "start_pos": {"x": 12, "y": 12, "rotation": 0}
}
```

**Generate AutoData.java**:
```bash
python3 json_to_java_converter.py
```

**RobotFunctions.java**:
```java
public void intake() {
    clawServo.setPosition(0.5);
    sleep(300);
    intakeMotor.setPower(0.8);
    sleep(1000);
    clawServo.setPosition(0.0);
}

public void score() {
    armMotor.setTargetPosition(2000);
    while (armMotor.isBusy()) sleep(10);
    clawServo.setPosition(0.5);
    sleep(500);
}
```

### Example 2: Continuous Intake While Moving

**path.json**:
```json
{
  "path": [
    {"x": 12, "y": 12},
    {"x": 24, "y": 12},
    {"x": 72, "y": 12},
    {"x": 96, "y": 12}
  ]
}
```

**functions.json**:
```json
{
  "functions": [
    {"name": "start_intake", "x": 24, "y": 12, "rotation": 0, "type": "run_while_moving", "action": "function"},
    {"name": "stop_intake", "x": 96, "y": 12, "rotation": 0, "type": "run_while_moving", "action": "function"}
  ],
  "templates": ["start_intake", "stop_intake"],
  "start_pos": {"x": 12, "y": 12, "rotation": 0}
}
```

**Generate AutoData.java**:
```bash
python3 json_to_java_converter.py
```

**RobotFunctions.java**:
```java
public void start_intake() {
    intakeMotor.setPower(0.8);
    // Returns immediately, motor keeps running while robot moves!
}

public void stop_intake() {
    intakeMotor.setPower(0);
}
```

### Example 3: Complex Autonomous Routine

**path.json**:
```json
{
  "path": [
    {"x": 120, "y": 16},
    {"x": 84, "y": 52},
    {"x": 84, "y": 60},
    {"x": 104, "y": 60},
    {"x": 120, "y": 60},
    {"x": 104, "y": 60},
    {"x": 84, "y": 60},
    {"x": 84, "y": 52},
    {"x": 96, "y": 84},
    {"x": 104, "y": 84},
    {"x": 120, "y": 84},
    {"x": 104, "y": 84},
    {"x": 96, "y": 84},
    {"x": 84, "y": 52},
    {"x": 92, "y": 108}
  ]
}
```

**functions.json**:
```json
{
  "functions": [
    {"name": "intake", "x": 104, "y": 60, "rotation": 0, "type": "run_while_moving", "action": "function"},
    {"name": "score", "x": 84, "y": 52, "rotation": 315, "type": "wait_till", "action": "function"},
    {"name": "intake", "x": 104, "y": 84, "rotation": 0, "type": "run_while_moving", "action": "function"}
  ],
  "templates": ["intake", "outtake", "score", "park"],
  "start_pos": {"x": 120, "y": 16, "rotation": 135}
}
```

**Note**: Same function name ("intake") can be used multiple times at different locations.

---

## Quick Reference

### Workflow
```
1. Edit functions.json and path.json
2. Run: python3 json_to_java_converter.py
3. Copy AutoData.java to project
4. Build & Deploy
```

### Function Types
```
run_while_moving  → Background, robot continues
wait_till         → Blocks, robot waits
```

### Add New Function
```
1. JSON: {"name": "my_func", ...}
2. Run converter
3. Copy AutoData.java to project
4. Java: public void my_func() { }
5. Build & Deploy
```

### File Locations
```
Workspace:        → functions.json, path.json, converter script
teamcode/kool/    → AutoData.java, AutoPathFollower.java, RobotFunctions.java
```

---

## Migration from Version 2.0

If you're upgrading from the JSON-based version:

### Changes Required:

1. **Remove assets folder** - No longer needed
2. **Add converter script** to your workspace
3. **Update AutoPathFollower.java** - Use version 3.0
4. **Update RobotFunctions.java** - Minor changes to comments
5. **Run converter** to generate AutoData.java
6. **Remove JSON dependencies** - No org.json imports

### Benefits:

- Faster startup (no JSON parsing)
- Compile-time error checking
- Better IDE support
- Type safety
- Smaller APK

---

**Documentation Version 3.0**
**Last Updated: December 2025**