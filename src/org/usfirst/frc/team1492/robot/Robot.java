package org.usfirst.frc.team1492.robot;

import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.interfaces.Gyro;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
    final String defaultAuto = "Default";
    final String customAuto = "My Auto";
    String autoSelected;
    SendableChooser chooser;
    CameraServer server;

    NetworkTable axisCam;

    Boolean useGamepad = true;

    Joystick[] joysticks;

    int axisCount, buttonCount;

    VictorSP leftDrive;
    VictorSP rightDrive;
    VictorSP conveyor;
    VictorSP shooter;
    VictorSP arm;
    VictorSP intake;
    VictorSP intakeArm;
    VictorSP lift;

    // Sensors
    Gyro gyro;

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        chooser = new SendableChooser();
        chooser.addDefault("Default Auto", defaultAuto);
        chooser.addObject("My Auto", customAuto);
        SmartDashboard.putData("Auto choices", chooser);

        axisCam = NetworkTable.getTable("SmartDashboard");

        joysticks = new Joystick[useGamepad ? 2 : 2];

        if (useGamepad) {
            joysticks[0] = new Joystick(0);
            joysticks[1] = new Joystick(1);
        } else {
            joysticks[0] = new Joystick(0);
            joysticks[1] = new Joystick(1);
        }

        leftDrive  = new VictorSP(0);
        rightDrive = new VictorSP(1);
        conveyor   = new VictorSP(2);
        shooter    = new VictorSP(3);
        arm        = new VictorSP(4);
        intake     = new VictorSP(5);
        intakeArm  = new VictorSP(6);
        lift       = new VictorSP(7);

        // Sensors
        gyro = new AnalogGyro(1);

        // Camera
        server = CameraServer.getInstance();
        server.setQuality(50);

        // Iterate through camera ids (0 to 3) to find camera (if connected)
        for (int i = 0; i < 4; i++) {
            server.startAutomaticCapture("cam" + i);
            if (server.isAutoCaptureStarted()) {
                System.out.println("Camera found at " + i);
                break;
            }
        }

        resetSensors();

        // Testing
        axisCount = joysticks[0].getAxisCount();
        buttonCount = joysticks[0].getButtonCount();
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different autonomous modes using the dashboard. The sendable
     * chooser code works with the Java SmartDashboard.
     *
     * You can add additional auto modes by adding additional comparisons to the
     * switch structure below with additional strings. If using the
     * SendableChooser make sure to add them to the chooser code above as well.
     */
    public void autonomousInit() {
        autoSelected = (String) chooser.getSelected();
        System.out.println("Auto selected: " + autoSelected);
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        switch (autoSelected) {
        case customAuto:
            // Put custom auto code here
            break;
        case defaultAuto:
        default:
            // Put default auto code here
            break;
        }
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        double leftSpeed = deadband(getMotor(0));
        double rightSpeed = deadband(getMotor(1));
        double blobVal = axisCam.getNumber("BLOB_COUNT", 0.0);


        SmartDashboard.putNumber("Cam Value Blob", blobVal);

        SmartDashboard.putNumber("left joy db", leftSpeed);
        SmartDashboard.putNumber("right joy db", rightSpeed);

        SmartDashboard.putBoolean("speed mapping", getSpeedMapping());

        if (getSpeedMapping()) {
            leftSpeed /= 2;
            rightSpeed /= 2;
        }

        /*
         * Button Mapping
         * 1: A
         * 2: B
         * 3: X
         * 4: Y
         * 5: Left Bumper
         * 6: Right Bumper
         * 7: Back
         * 8: Start
         * 9: Left Joystick
         * 10: Right Joystick
         */
        
        // Driver Remote
        /**
         * Invert left y axis so motor turns in the correct direction. The left
         * and right sides have to be inverted because the motors are mirrored
         */
        leftDrive.set(-leftSpeed);
        rightDrive.set(rightSpeed);
        
        boolean conveyorButton = false;
        if (!conveyorButton) {
        	conveyorButton = true;
        	if (joysticks[0].getRawButton(6)) conveyor.set(100);
        } else {
        	conveyorButton = false;
        	if (joysticks[0].getRawButton(6)) conveyor.set(0);
        }
        
        boolean intakeButton = false;
        if (!intakeButton) {
        	intakeButton = true;
        	if (joysticks[0].getRawButton(5)) intake.set(100);
        } else {
        	intakeButton = false;
        	if (joysticks[0].getRawButton(5)) intake.set(0);
        }
        
        // Partner Remote
        if (joysticks[1].getRawButton(3)) shooter.set(100);
        if (joysticks[1].getRawButton(2)) shooter.set(0);
        
        if (joysticks[1].getRawButton(4)) arm.set(50);
        if (joysticks[1].getRawButton(1)) arm.set(-50);
        
        intakeArm.set(joysticks[1].getRawAxis(5));
        
        if (joysticks[1].getRawButton(7)) lift.set(100);
        if (joysticks[1].getRawButton(8)) lift.set(-100);
        
        SmartDashboard.putNumber("right motor", rightSpeed);
        SmartDashboard.putNumber("left motor", leftSpeed);

        double angle = gyro.getAngle();
        SmartDashboard.putNumber("Gyro", angle);

    }

    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        String joyData = "";
        String buttonData = "";

        for (int i = 0; i < axisCount; i++) {
            joyData = joyData + String.format("axis %d is %b  ", i, joysticks[0].getRawAxis(i));
        }
        for (int i = 0; i < buttonCount; i++) {
            buttonData = buttonData + String.format("button %d is %b  ", i + 1, joysticks[0].getRawAxis(i + 1));
        }

        System.out.println(joyData);
        System.out.println(buttonData);

        Timer.delay(0.2);
    }

    double deadband(double rawValue) {
        return deadband(rawValue, 0.2);
    }

    double deadband(double rawValue, double deadspace) {
        if (rawValue > deadspace) {
            return (rawValue - deadspace) / (1 - deadspace);
        }
        if (rawValue < -deadspace) {
            return (rawValue + deadspace) / (1 - deadspace);
        }
        return 0;
    }

    /**
     * Returns the y value of the joystick for the respective motor.
     *
     * @param side 0 for left 1 for right
     * @return value of joystick for given motor
     */
    double getMotor(int side) {
        if (useGamepad) {
            return joysticks[0].getRawAxis(side * 4 + 1);
        } else {
            return joysticks[side].getAxis(Joystick.AxisType.kY);
        }
    }

    /**
     * Determines whether to use the reduced speed mode
     *
     * @return true if reduced speed, false if not
     */
    boolean getSpeedMapping() {
        if (useGamepad) {
            return joysticks[0].getRawAxis(3) > 0.5 ? true : false;
        } else {
            // TODO: check if Joystick.ButtonType.kTrigger works.
            return joysticks[1].getRawButton(1);
        }
    }

    // Reset sensors
    void resetSensors() {
        gyro.reset();
    }

}
