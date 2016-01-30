package org.usfirst.frc.team1492.robot;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
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

    //xbox remote
    Joystick controller;
    
    //Double Joystick
    //Joystick stickLeft;
    //Joystick stickRight;

    int axisCount, buttonCount;

    VictorSP leftMotor, rightMotor;

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        chooser = new SendableChooser();
        chooser.addDefault("Default Auto", defaultAuto);
        chooser.addObject("My Auto", customAuto);
        SmartDashboard.putData("Auto choices", chooser);

        //xbox remote
        controller = new Joystick(0);
        //double joysticks
        //stickLeft = new Joystick(0);
        //stickRight = new Joystick(1);
        
        leftMotor = new VictorSP(0);
        rightMotor = new VictorSP(1);


        try {
            server = CameraServer.getInstance();
            server.setQuality(50);
            server.startAutomaticCapture("cam0");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Testing
        //axisCount = controller.getAxisCount();
        //buttonCount = controller.getButtonCount();
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different autonomous modes using the dashboard. The sendable
     * chooser code works with the Java SmartDashboard. If you prefer the
     * LabVIEW Dashboard, remove all of the chooser code and uncomment the
     * getString line to get the auto name from the text box below the Gyro
     *
     * You can add additional auto modes by adding additional comparisons to the
     * switch structure below with additional strings. If using the
     * SendableChooser make sure to add them to the chooser code above as well.
     */
    public void autonomousInit() {
        autoSelected = (String) chooser.getSelected();
        // autoSelected = SmartDashboard.getString("Auto Selector",
        // defaultAuto);
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
    	//xbox remote
        // The controller y axises are -1 when up and 1 when down
        double leftStick = controller.getRawAxis(1);
        double rightStick = controller.getRawAxis(5);

    	//Double joysick
    	//double leftStick = stickLeft.getRawAxis(1);
    	//double rightStick = stickRight.getRawAxis(1);
        /**
         * Invert left y axis so motor turns in the correct direction. The left
         * and right sides have to be inverted because the motors are mirrored
         */
        leftStick = -leftStick;

        leftMotor.set(deadband(leftStick));
        rightMotor.set(deadband(rightStick));
        
        square(leftStick);
        square(rightStick);
      
    }

    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        /*String joyData = "";
        String buttonData = "";

        for (int i = 0; i < axisCount; i++) {
            joyData = joyData + String.format("axis %d is %b  ", i, controller.getRawAxis(i));
        }
        for (int i = 0; i < buttonCount; i++) {
            buttonData = buttonData + String.format("button %d is %b  ", i + 1, controller.getRawAxis(i + 1));
        }

        System.out.println(joyData);
        System.out.println(buttonData);*/
        Timer.delay(0.2);
    }

    double deadband(double rawValue) {
        return deadband(rawValue, 0.2);
    }

    double deadband(double rawValue, double deadspace) {
        if (rawValue > deadspace) {
            return (rawValue - deadspace) / deadspace;
        }
        if (rawValue < -deadspace) {
            return (rawValue + deadspace) / deadspace;
        }
        return 0;
    }

    double abs(double a) {
    	if (a < 0) {
    		return -a;
    	} else {
    		return a;
    	}
    }
    
    double square(double a) {
    	return a * a;
    }
}
