# FTC 2025 Field-Centric Autonomous Path Follower
## Soft-Coded JSON-Based Navigation System

**Version 2.0** - Complete rewrite with background threading and true soft-coding

---

## 📋 Table of Contents
- [Overview](#overview)
- [Changelog](#changelog)
- [Features](#features)
- [Quick Start](#quick-start)
- [File Structure](#file-structure)
- [Understanding the System](#understanding-the-system)
- [JSON File Format](#json-file-format)
- [Creating Custom Functions](#creating-custom-functions)
- [Configuration](#configuration)
- [Advanced Usage](#advanced-usage)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)

---

## 📖 Overview

This autonomous system allows you to define robot paths and actions using JSON files, with true soft-coded function execution using Java reflection. The robot follows waypoints while executing custom functions either in the background (while moving) or blocking (waiting for completion).

### Key Capabilities:
- ✅ Field-centric mecanum drive with IMU
- ✅ Encoder-based movement verification (auto-fallback to time-based)
- ✅ **Background threading** for run-while-moving functions
- ✅ **True soft-coding** - functions discovered automatically by name
- ✅ JSON-defined paths and function triggers
- ✅ Three function execution modes

---

## 📝 Changelog

### Version 2.0 (Current)
**Major Rewrite - Background Threading & True Soft-Coding**

#### Breaking Changes:
- ✨ **NEW**: `run_while_moving` functions now execute in background threads
    - Functions can include sleep() and delays without blocking robot movement
    - Robot continues to next waypoint while function executes
- ✨ **NEW**: True soft-coded function dispatch using Java reflection
    - No more switch statements or if/else blocks
    - Just create methods matching JSON names
- ✨ **CHANGED**: Removed `startFunction()` and `stopFunction()` methods
    - All functions use single `executeFunction()` call
    - Background vs blocking determined by JSON type, not function design

#### Improvements:
- 🔧 Simplified RobotFunctions class
- 🔧 Better error handling with Exception catching
- 🔧 Clearer execution flow: Move → Check position → Execute function
- 🔧 Updated package to `org.firstinspires.ftc.teamcode.kool`
- 📚 Comprehensive documentation

#### Bug Fixes:
- 🐛 Fixed JSON exception handling
- 🐛 Removed unused parameters causing warnings
- 🐛 Fixed function coordinate matching logic

### Version 1.0 (Initial)
- Basic path following with JSON
- Hard-coded switch statements for functions
- Sequential execution only
- Manual function start/stop

---

## 🌟 Features

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
- **JSON-Based**: Easy path and function editing without recompiling
- **Asset Bundling**: All files bundled in APK via Android Studio
- **Hot-Swappable**: Change JSON files and rebuild - no code changes needed

---

## 🚀 Quick Start

### 1. File Locations

```
TeamCode/
├── src/
│   └── main/
│       ├── assets/                    ← CREATE THIS
│       │   ├── functions.json         ← PUT HERE
│       │   └── path.json              ← PUT HERE
│       └── java/
│           └── org/firstinspires/ftc/teamcode/kool/
│               ├── AutoPathFollower.java     ← PUT HERE
│               └── RobotFunctions.java       ← PUT HERE
```

### 2. Create Assets Folder
1. In Android Studio: Right-click `TeamCode/src/main/`
2. Select `New` → `Directory`
3. Name it: `assets`

### 3. Add JSON Files
Create `functions.json` and `path.json` in the assets folder

### 4. Add Java Files
Copy both Java files to the `kool` package

### 5. Build & Deploy
1. Click `Build` → `Make Project`
2. Connect to Robot Controller
3. Click Run ▶

---

## 📁 File Structure

### Required Files

| File | Location | Purpose |
|------|----------|---------|
| `AutoPathFollower.java` | `teamcode/kool/` | Main autonomous OpMode |
| `RobotFunctions.java` | `teamcode/kool/` | Your custom functions |
| `functions.json` | `assets/` | Function definitions & coordinates |
| `path.json` | `assets/` | Robot waypoint path |

---

## 🧠 Understanding the System

### Core Concept: Functions are Markers

**Important**: Function coordinates in `functions.json` do NOT define where the robot moves!

- **`path.json`** defines WHERE the robot moves (waypoints)
- **`functions.json`** defines WHAT to do when robot is at specific positions (markers)

### Execution Flow

```
1. Load path.json and functions.json from assets
2. Set starting position
3. For each waypoint in path:
   a. Move to waypoint coordinates
   b. Update current position
   c. Check: Is there a function at this position?
   d. If yes:
      - "run_while_moving" → Start in background, continue immediately
      - "wait_till" + "function" → Execute and wait for completion
      - "wait_till" + "rotate_only" → Rotate and wait
   e. Continue to next waypoint
4. Path complete
```

### The Three Function Types

#### Type 1: "run_while_moving"
Robot arrives → Start function in background thread → Continue moving immediately

**Use for**: Starting intake/outtake while driving

**Example**:
```json
{"name": "intake", "x": 36, "y": 84, "type": "run_while_moving", "action": "function"}
```

```java
public void intake() {
    intakeMotor.setPower(0.8);
    sleep(2000);  // Robot keeps moving during this!
    intakeMotor.setPower(0);
}
```

#### Type 2: "wait_till" + "function"
Robot arrives → Execute function → Wait for completion → Continue

**Use for**: Scoring, complex sequences

**Example**:
```json
{"name": "score", "x": 96, "y": 24, "type": "wait_till", "action": "function"}
```

```java
public void score() {
    armMotor.setTargetPosition(1000);
    sleep(1000);  // Robot waits here
    clawServo.setPosition(0.5);
}
```

#### Type 3: "wait_till" + "rotate_only"
Robot arrives → Rotate to angle → Wait → Continue

**Use for**: Aligning for scoring

**Example**:
```json
{"name": "rotate", "x": 72, "y": 24, "rotation": 90, "type": "wait_till", "action": "rotate_only"}
```

**No Java function needed!**

---

## 📄 JSON File Format

### path.json

```json
{
  "path": [
    {"x": 84, "y": 132},
    {"x": 108, "y": 108},
    {"x": 36, "y": 84},
    {"x": 96, "y": 24}
  ]
}
```

- `x`, `y`: Coordinates in inches (0-144 on standard field)
- Robot follows in order

### functions.json

```json
{
  "functions": [
    {
      "name": "intake",
      "x": 36,
      "y": 84,
      "rotation": 180,
      "type": "run_while_moving",
      "action": "function"
    }
  ],
  "start_pos": {
    "x": 84,
    "y": 132,
    "rotation": 315
  }
}
```

**Parameters**:
- `name`: Must match Java method name exactly
- `x`, `y`: Must match a waypoint in path.json
- `rotation`: Target angle in degrees
- `type`: `"run_while_moving"` or `"wait_till"`
- `action`: `"function"` or `"rotate_only"`

**Critical**: Coordinates must match waypoints within 0.1 inches!

---

## 🔧 Creating Custom Functions

### Step 1: Add to functions.json
```json
{"name": "my_function", "x": 48, "y": 96, "type": "wait_till", "action": "function"}
```

### Step 2: Create Method in RobotFunctions.java
```java
public void my_function() {
    // Your code here
    clawServo.setPosition(0.5);
    sleep(300);
}
```

### That's It!
Auto-discovered by name - no switch statements needed!

### Guidelines

**For "run_while_moving"**:
```java
public void start_intake() {
    intakeMotor.setPower(0.8);
    sleep(2000);  // OK! Runs in background
    intakeMotor.setPower(0);
}
```

**For "wait_till"**:
```java
public void score() {
    armMotor.setTargetPosition(2000);
    while (armMotor.isBusy()) sleep(10);  // Robot waits
    clawServo.setPosition(0.5);
}
```

---

## ⚙️ Configuration

### Hardware Names
```java
frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
frontRight = hardwareMap.get(DcMotor.class, "frontRight");
backLeft = hardwareMap.get(DcMotor.class, "backLeft");
backRight = hardwareMap.get(DcMotor.class, "backRight");
imu = hardwareMap.get(IMU.class, "imu");
```

### Constants
```java
COUNTS_PER_MOTOR_REV = 145.1;    // Adjust for your motors
WHEEL_DIAMETER_INCHES = 4.0;     // Measure your wheels
DRIVE_SPEED = 0.6;               // Tune for accuracy
POSITION_TOLERANCE = 2.0;        // Arrival threshold
```

---

## 🐛 Troubleshooting

### Functions Not Executing
1. ✅ Verify coordinates match exactly (path vs functions)
2. ✅ Check function name matches method name (case-sensitive)
3. ✅ Look for "Function not found" in telemetry
4. ✅ Ensure JSON files in `assets/` folder

### Robot Not Moving
1. ✅ Check motor directions
2. ✅ Verify encoder connections
3. ✅ Adjust `COUNTS_PER_MOTOR_REV`
4. ✅ Check wheel diameter

### JSON Errors
1. ✅ Validate at https://jsonlint.com
2. ✅ Check commas and brackets
3. ✅ Use double quotes only
4. ✅ Clean & Rebuild Project

---

## 📚 Examples

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
    {"name": "intake", "x": 36, "y": 12, "type": "wait_till", "action": "function"},
    {"name": "score", "x": 36, "y": 96, "type": "wait_till", "action": "function"}
  ],
  "start_pos": {"x": 12, "y": 12, "rotation": 0}
}
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

### Example 2: Continuous Intake

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
    {"name": "start_intake", "x": 24, "y": 12, "type": "run_while_moving", "action": "function"},
    {"name": "stop_intake", "x": 96, "y": 12, "type": "run_while_moving", "action": "function"}
  ],
  "start_pos": {"x": 12, "y": 12, "rotation": 0}
}
```

**RobotFunctions.java**:
```java
public void start_intake() {
    intakeMotor.setPower(0.8);
    // Returns immediately, motor keeps running!
}

public void stop_intake() {
    intakeMotor.setPower(0);
}
```

---

## 🎯 Quick Reference

### File Locations
```
assets/  → functions.json, path.json
kool/    → AutoPathFollower.java, RobotFunctions.java
```

### Function Types
```
run_while_moving  → Background, robot continues
wait_till         → Blocks, robot waits
rotate_only       → Auto-rotation
```

### Add New Function
```
1. JSON: {"name": "my_func", ...}
2. Java: public void my_func() { }
3. Done!
```

---

**Happy Autonomous! 🤖**