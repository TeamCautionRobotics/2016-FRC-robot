package org.usfirst.frc.team1492.robot;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {

    final boolean cameraConnected = true;

    CameraServer server;

    Joystick[] joysticks;

    VictorSP rightDrive;
    VictorSP leftDrive;
    VictorSP conveyor;
    VictorSP shooter;
    VictorSP intake;
    VictorSP intakeArm;
    VictorSP cameraLight;

    Relay statusLightsRelay;

    boolean cameraLightOn = false;
    boolean cameraLightPressed = false;
    boolean shooterControllerEnabled = false;

    Encoder shooterEncoder;
    
    enum AutoState {
    	INITAL,
    	DRIVING,
    	FINISHED
    }
    
    AutoState autoState;
    
    Timer autoTimer;

    DigitalInput armBack;
    DigitalInput ballLoaded;
    DigitalInput intakeArmDown;
    DigitalInput armForward;

    PIDController shooterController;

    Preferences shooterPrefs;

    boolean autoMode = true;
    
    final static String slowAutoString = "slow auto";
    double autoDriveTime = 0;

    class Buttons {
        public final static int A = 1;
        public final static int B = 2;
        public final static int X = 3;
        public final static int Y = 4;

        // Above analog triggers
        public final static int LEFT_BUMPER = 5, RIGHT_BUMPER = 6;

        public final static int BACK = 7, START = 8;

        // Joystick click
        public final static int LEFT_JOYSTICK = 9, RIGHT_JOYSTICK = 10;
    }

    class Axises {
        public final static int LEFT_X = 0, LEFT_Y = 1;
        public final static int LEFT_TRIGGER = 2, RIGHT_TRIGGER = 3;
        public final static int RIGHT_X = 4, RIGHT_Y = 5;
    }

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        joysticks = new Joystick[2];
        joysticks[0] = new Joystick(0);
        joysticks[1] = new Joystick(1);

        rightDrive = new VictorSP(0);
        leftDrive = new VictorSP(1);
        conveyor = new VictorSP(2);
        shooter = new VictorSP(3);
        intakeArm = new VictorSP(4);
        intake = new VictorSP(6);

        statusLightsRelay = new Relay(0);

        initalizeMotorSpeeds();

        ballLoaded = new DigitalInput(0);
        intakeArmDown = new DigitalInput(1);
        armBack = new DigitalInput(2);
        armForward = new DigitalInput(3);

        shooterEncoder = new Encoder(5, 6, true, Encoder.EncodingType.k4X);
        shooterEncoder.setPIDSourceType(PIDSourceType.kRate);
        shooterEncoder.setDistancePerPulse(1.0/1024);

        shooterController = new PIDController(1, 1, 1, 1, shooterEncoder, shooter);
        shooterController.setOutputRange(0, 1);
        shooterController.setInputRange(-115, 115);
        shooterController.setSetpoint(0);
        shooterController.setToleranceBuffer(4);
        shooterControllerEnabled(false);

        shooterPrefs = Preferences.getInstance();
        shooterPrefs.getDouble("P", 0.0005);
        shooterPrefs.getDouble("I", 0.005);
        shooterPrefs.getDouble("D", 0);
        shooterPrefs.getDouble("F", 0.001);
        shooterPrefs.putDouble("shootspeed", 98);
        
        SmartDashboard.putBoolean(slowAutoString, false);
        SmartDashboard.getBoolean(slowAutoString, false);


        // Camera
        if (cameraConnected) {
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
        }
    }

    public void autonomousInit() {
    	autoState = AutoState.INITAL;
    	autoTimer = new Timer();
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        switch (autoState) {
        	case INITAL: {
        		autoState = AutoState.DRIVING;
        		if (SmartDashboard.getBoolean(slowAutoString)) {
        		    autoDriveTime = 7;
                    setDrive(0.4);
        		} else {
                    autoDriveTime = 3.7;
                    setDrive(0.8);
        		}

        		autoTimer.reset();
                autoTimer.start();
        		break;
        	}
			case DRIVING: {
				if(autoTimer.get() >= autoDriveTime){
    				setDrive(0);
    				autoState = AutoState.FINISHED;
				}
				break;
			}
			case FINISHED: {
				// Lalala done
				break;
			}
		default:
			break;
        }
    }

    @Override
    public void teleopInit() {
        shooterController.setSetpoint(0);
        shooterControllerEnabled(true);
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
    	/*
    	 * Main:
    	 * Drive = both joysticks Y
    	 * all out = left trigger
    	 * Turn on targeting light = A
    	 *
    	 * Partner:
    	 * conveyor out = left bumper
    	 * conveyor in = right bumper
    	 * shooter fire = right trigger
    	 * intake arm = left joystick Y
    	 * intake roller = right joystick Y
    	 * all out = B
    	 */

        updateShooterPIDGains();
        updateShooterSpeedReport();

        double leftSpeed = -deadband(getMotor(Side.LEFT));
        double rightSpeed = -deadband(getMotor(Side.RIGHT));

        SmartDashboard.putNumber("left joy db", leftSpeed);
        SmartDashboard.putNumber("right joy db", rightSpeed);

        setDrive(leftSpeed, rightSpeed);

        // The camera light button is not the same as what it was
        // (it is pressed or unpressed)
        boolean cameraLightButton = joysticks[0].getRawButton(Buttons.A);
        if (cameraLightButton != cameraLightPressed) {
            if (cameraLightButton) {
                cameraLightOn = !cameraLightOn;
            }
            cameraLightPressed = cameraLightButton;
        }

        // Run full eject of boulder. Intake, conveyor, and shooter all reverse
        // This overrides all other boulder transport commands
        if (joysticks[0].getRawAxis(Axises.LEFT_TRIGGER) > 0.5 || joysticks[1].getRawButton(Buttons.B)) {
            moveBoulder(Directions.OUT);
        } else {
            double intakeJoystickPosition = deadband(joysticks[1].getRawAxis(Axises.RIGHT_Y)/-2);
            intake.set(intakeJoystickPosition);

            // Conveyor - left bumper out; left trigger in
            Directions conveyorDirection = Directions.STOP;
            if (joysticks[1].getRawButton(Buttons.LEFT_BUMPER)) {
                conveyorDirection = Directions.OUT;
            } else if (joysticks[1].getRawButton(Buttons.RIGHT_BUMPER)) {
                conveyorDirection = Directions.IN;
            } else {
            	conveyorDirection = Directions.STOP;
            }

            moveConveyor(conveyorDirection);

            // Shooter
            runShooter(joysticks[1].getRawAxis(Axises.RIGHT_TRIGGER) > 0.5);
        }

        double intakeArmSpeed = deadband(joysticks[1].getRawAxis(Axises.LEFT_Y));
    	intakeArm.set(intakeArmSpeed);
    	SmartDashboard.putNumber("Intake arm motor", intakeArmSpeed);

        updateStatusLights(cameraLightOn, shooterAtShootSpeed());

        updateDSLimitSW();
    }

    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {

    }


    void runShooter(boolean on) {
        if (!shooterControllerEnabled) {
            shooterControllerEnabled(true);
        }
        if (on) {
            double shootSpeed = shooterPrefs.getDouble("shootspeed", 98);
            shooterController.setSetpoint(shootSpeed);
        } else {
            shooterController.setSetpoint(0);
        }
    }
    
    boolean shooterAtShootSpeed() {
        double shootSpeed = shooterPrefs.getDouble("shootspeed", 98);
        return Math.abs(shooterEncoder.getRate() - shootSpeed) <= 5;
    }
    
    void updateShooterPIDGains() {
        double p = shooterPrefs.getDouble("P", 0.0005);
        double i = shooterPrefs.getDouble("I", 0.005);
        double d = shooterPrefs.getDouble("D", 0);
        double f = shooterPrefs.getDouble("F", 0.001);

        shooterController.setPID(p, i, d, f);
    }
    
    void updateShooterSpeedReport() {
        double shooterSpeed = shooterEncoder.getRate();
        shooterPrefs.putDouble("speed", shooterSpeed);
        SmartDashboard.putNumber("sspeed", shooterSpeed);
    }

    void shooterControllerEnabled(boolean isEnabled) {
        if (isEnabled == shooterControllerEnabled) {
            return;
        } else {
            if (isEnabled) {
                shooterController.enable();
                shooterControllerEnabled = true;
            } else {
                shooterController.disable();
                shooterControllerEnabled = false;
            }
        }
    }

    // Debug functions
    void updateDSLimitSW() {
        SmartDashboard.putBoolean("Limit Switches armBack", armBack.get());
        SmartDashboard.putBoolean("Limit Switches ballLoaded", ballLoaded.get());
        SmartDashboard.putBoolean("Limit Switches intakeArmDown", intakeArmDown.get());
        SmartDashboard.putBoolean("Limit Switches armForward", armForward.get());
    }

    void initalizeMotorSpeeds() {
        SmartDashboard.getNumber("Shooter forward speed",   1);
        SmartDashboard.getNumber("Shooter backward speed",  1);
        SmartDashboard.getNumber("Conveyor forward speed",  1);
        SmartDashboard.getNumber("Conveyor backward speed", 1);
        SmartDashboard.getNumber("Intake forward speed",    1);
        SmartDashboard.getNumber("Intake backward speed",   1);

        SmartDashboard.getNumber("Lift extend speed",       1);
        SmartDashboard.getNumber("Lift retract speed",      1);

        SmartDashboard.getNumber("Defense arm max speed",   1);
        SmartDashboard.getNumber("Intake arm max speed",    1);
    }


    void updateStatusLights(boolean cameraLight, boolean poleLight) {
        if (poleLight && cameraLight) {
            statusLightsRelay.set(Relay.Value.kOn);
        } else if (cameraLight) {
            statusLightsRelay.set(Relay.Value.kForward);
        } else if (poleLight) {
            statusLightsRelay.set(Relay.Value.kReverse);
        } else {
            statusLightsRelay.set(Relay.Value.kOff);
        }
    }


    enum Directions {
        STOP, IN, OUT
    }

    void moveIntake(Directions direction) {
        switch (direction) {
        case STOP:
            intake.set(0);
            break;
        case IN:
            intake.set(-SmartDashboard.getNumber("Intake forward speed", 1));
            break;
        case OUT:
            intake.set(SmartDashboard.getNumber("Intake backward speed", 0.6));
            break;
        }
    }

    void moveConveyor(Directions direction) {
        switch (direction) {
        case STOP:
            conveyor.set(0);
            break;
        case IN:
            conveyor.set(SmartDashboard.getNumber("Conveyor forward speed", 1));
            break;
        case OUT:
            conveyor.set(-SmartDashboard.getNumber("Conveyor backward speed", 1));
            break;
        }
    }

    void moveShooter(Directions direction) {
        switch (direction) {
        case STOP:
            shooterController.setSetpoint(0);
            break;
        case IN:
            shooterControllerEnabled(true);
            runShooter(true);
            break;
        case OUT:
            shooterControllerEnabled(false);
            shooter.set(-0.5);
            break;
        }
    }

    void moveBoulder(Directions boulderDirection) {
        moveBoulder(boulderDirection, boulderDirection, boulderDirection);
    }

    void moveBoulder(Directions intakeDirection, Directions conveyorDirection, Directions shooterDirection) {
        moveIntake(intakeDirection);
        moveConveyor(conveyorDirection);
        moveShooter(shooterDirection);
    }


    // Robot drive system utility functions:
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
     * Returns the Y value of the joystick for the respective motor.
     *
     * @param side false for left  true for right
     * @return value of joystick for given motor
     */
    double getMotor(boolean side) {
        return joysticks[0].getRawAxis(side ? Axises.RIGHT_Y : Axises.LEFT_Y);
    }

    class Side {
        public final static boolean LEFT = false;
        public final static boolean RIGHT = true;
    }


    /**
     * Invert left side so the motor turns in the correct direction. The left
     * and right sides have to be inverted because the motors are mirrored
     */

    void setDrive(double left, double right) {
        SmartDashboard.putNumber("right motor", right);
        SmartDashboard.putNumber("left motor", left);
        leftDrive.set(left);
        rightDrive.set(-right);
    }

    void setDrive(double speed) {
        setDrive(speed, speed);
    }
}
