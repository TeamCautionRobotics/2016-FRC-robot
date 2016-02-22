package org.usfirst.frc.team1492.robot;

import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DigitalInput;
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
    final String noAuto   = "Nothing";
    final String moatAuto = "Moat";

    String autoSelected;
    SendableChooser autoChooser;

    CameraServer server;

    NetworkTable axisCam;

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

    DigitalInput[] limitSwitches;

    // Sensors
    Gyro gyro;

    DigitalInput armBack;
    DigitalInput ballLoaded;
    DigitalInput intakeArmDown;
    DigitalInput armForward;

    class Buttons {
        public final static int A = 1;
        public final static int B = 2;
        public final static int X = 3;
        public final static int Y = 4;
        // Above analog trigger
        public final static int LEFT_BUMPER = 5, RIGHT_BUMPER = 6;
        public final static int BACK = 7, START = 8;
        // Joystick click
        public final static int LEFT_JOYSTICK = 9, RIGHT_JOYSTICK = 10;
    }

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        axisCam = NetworkTable.getTable("SmartDashboard");

        joysticks = new Joystick[2];
        joysticks[0] = new Joystick(0);
        joysticks[1] = new Joystick(1);

        leftDrive = new VictorSP(0);
        rightDrive = new VictorSP(1);
        conveyor = new VictorSP(2);
        shooter = new VictorSP(3);
        arm = new VictorSP(4);
        intake = new VictorSP(5);
        // intakeArm = new VictorSP(6);
        lift = new VictorSP(7);

        autoChooser = new SendableChooser();
        autoChooser.addDefault("Nothing", noAuto);
        autoChooser.addObject("Moat", moatAuto);

        SmartDashboard.putData("Autonomouse mode chooser", autoChooser);

        // Sensors
        gyro = new AnalogGyro(1);

        armBack = new DigitalInput(1);
        ballLoaded = new DigitalInput(2);
        intakeArmDown = new DigitalInput(3);
        armForward = new DigitalInput(4);

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
        autoSelected = (String) autoChooser.getSelected();
        System.out.println("Auto selected: " + autoSelected);
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        switch (autoSelected) {
        case moatAuto:
            drive(.5);
            Timer.delay(2);
            drive(0);
            break;
        case noAuto:
        default:
            // No auto
            break;
        }
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        double leftSpeed = deadband(getMotor(Side.LEFT));
        double rightSpeed = deadband(getMotor(Side.RIGHT));
        double blobVal = axisCam.getNumber("BLOB_COUNT", 0.0);

        SmartDashboard.putNumber("Cam Value Blob", blobVal);

        SmartDashboard.putNumber("left joy db", leftSpeed);
        SmartDashboard.putNumber("right joy db", rightSpeed);

        SmartDashboard.putBoolean("speed mapping", getSpeedMapping());

        if (getSpeedMapping()) {
            leftSpeed /= 2;
            rightSpeed /= 2;
        }

        // Driver Remote
        /**
         * Invert left y axis so motor turns in the correct direction. The left
         * and right sides have to be inverted because the motors are mirrored
         */
        leftDrive.set(leftSpeed);
        rightDrive.set(-rightSpeed);

        boolean reverse = joysticks[0].getRawAxis(2) > .2;
        if (joysticks[0].getRawButton(Buttons.LEFT_BUMPER)) {
            intake.set(0.5);
        } else if (reverse) {
            intake.set(-1);
        } else {
            intake.set(0);
        }

        // Partner

        if (joysticks[1].getRawButton(Buttons.A)) {
            shooter.set(1);
        } else if (reverse) {
            shooter.set(-1);
        } else {
            shooter.set(0);
        }

        if (joysticks[1].getRawButton(Buttons.RIGHT_BUMPER)) {
            conveyor.set(0.5);
        } else if (reverse) {
            conveyor.set(-1);
        } else {
            conveyor.set(0);
        }

        if (joysticks[0].getRawButton(Buttons.RIGHT_BUMPER)) {
            conveyor.set(0.5);
        } else if (reverse) {
            conveyor.set(-1);
        } else {
            conveyor.set(0);
        }

        // - forward
        // + backward
        // go + if back is not pressed
        // go - if forward is not pressed

        if (!armBack.get()) {
            arm.set(Math.min(0, deadband(joysticks[1].getRawAxis(1))));
        } else if (!armForward.get()) {
            arm.set(Math.max(0, deadband(joysticks[1].getRawAxis(1))));
        } else {
            arm.set(deadband(joysticks[1].getRawAxis(1)));
        }

        // if ((!armBack.get() && joysticks[1].getRawAxis(1) < 0) ||
        // (!armForward.get() && joysticks[1].getRawAxis(1) > 0)) {
        // arm.set(deadband(joysticks[1].getRawAxis(1)) / 4);
        // }
        // TODO: Move this or lift to different axis on controller
        // intakeArm.set(deadband(joysticks[1].getRawAxis(5)));
        //
        // if (joysticks[1].getRawButton(7)) lift.set(1);
        // if (joysticks[1].getRawButton(8)) lift.set(-1);

        lift.set(deadband(joysticks[1].getRawAxis(5)));

        SmartDashboard.putNumber("right motor", rightSpeed);
        SmartDashboard.putNumber("left motor", leftSpeed);

        double angle = gyro.getAngle();
        SmartDashboard.putNumber("Gyro", angle);

        testPeriodic();
    }

    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        SmartDashboard.putBoolean("Limit Switches armBack", armBack.get());
        SmartDashboard.putBoolean("Limit Switches ballLoaded", ballLoaded.get());
        SmartDashboard.putBoolean("Limit Switches intakeArmDown", intakeArmDown.get());
        SmartDashboard.putBoolean("Limit Switches armForward", armForward.get());
        SmartDashboard.putBoolean("Ball Loaded", ballLoaded.get());
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
     * @param side false for left  true for right
     * @return value of joystick for given motor
     */
    double getMotor(boolean side) {
        return joysticks[0].getRawAxis(side ? 0 : 5);
    }

    class Side {
        public final static boolean LEFT = false;
        public final static boolean RIGHT = true;
    }
    /**
     * Determines whether to use the reduced speed mode
     *
     * @return true if reduced speed, false if not
     */
    boolean getSpeedMapping() {
        return joysticks[0].getRawButton(Buttons.LEFT_JOYSTICK);
    }

    // Reset sensors
    void resetSensors() {
        gyro.reset();
    }

    void drive(double left, double right) {
        leftDrive.set(left);
        rightDrive.set(right);
    }

    void drive(double speed) {
        leftDrive.set(speed);
        rightDrive.set(speed);
    }
}
