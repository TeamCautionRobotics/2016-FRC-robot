package org.usfirst.frc.team1492.robot;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
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

    final String shooterThresholdName = "Shooter ready threshold (pulses/second)";

    int desiredShooterSpeed = 87000;

    CameraServer server;

    NetworkTable axisCam;

    Joystick[] joysticks;

    VictorSP rightDrive;
    VictorSP leftDrive;
    VictorSP conveyor;
    VictorSP shooter;
    VictorSP intake; //Old Arm
    //Old VictorSP intake;
    VictorSP intakeArm;
    VictorSP cameraLight;

    Relay flagLightsRelay;

    boolean ballWasIn   = false;
    boolean ballLightOn = false;

    boolean shooterWasReady = false;
    boolean shooterLightOn  = false;
    
    boolean cameraLightOn = false;

    Timer ballLightTimer;
    Timer shooterLightTimer;

    Encoder shooterEncoder;
    
    enum AutoState {
    	INITAL,
    	DRIVING,
    	FINISHED
    }
    
    AutoState autoState;
    
    Timer autoTimer;
    
    //final boolean autoLowerSArm = true;

    boolean autoFailed = false;

    DigitalInput armBack;
    DigitalInput ballLoaded;
    DigitalInput intakeArmDown;
    DigitalInput armForward;

    double BLOB_COUNT;
    double COG_BOX_SIZE;
    double COG_X;

    boolean autoMode = true;

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
        axisCam = NetworkTable.getTable("SmartDashboard");

        BLOB_COUNT = axisCam.getNumber("BLOB_COUNT", 0.0);
        COG_BOX_SIZE = axisCam.getNumber("COG_BOX_SIZE", 0.0);
        COG_X = axisCam.getNumber("COG_X", 0.0);


        joysticks = new Joystick[2];
        joysticks[0] = new Joystick(0);
        joysticks[1] = new Joystick(1);

        rightDrive = new VictorSP(0);
        leftDrive = new VictorSP(1);
        conveyor = new VictorSP(2);
        shooter = new VictorSP(3);
        intake = new VictorSP(4);
        //Old intake = new VictorSP(5);
        intakeArm = new VictorSP(6);
        cameraLight = new VictorSP(7);

        flagLightsRelay = new Relay(0);

        ballLightTimer = new Timer();
        shooterLightTimer = new Timer();

        shooterEncoder = new Encoder(5, 6, true, Encoder.EncodingType.k4X);

        initalizeMotorSpeeds();

        ballLoaded = new DigitalInput(0);
        intakeArmDown = new DigitalInput(1);
        armBack = new DigitalInput(2);
        armForward = new DigitalInput(3);

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
    	autoState = AutoState.INITAL;
    	autoTimer = new Timer();
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        switch (autoState) {
        	case INITAL:{
        		intakeArm.set(1);
        		//autoState = AutoState.LOWERING_INTAKE_ARM;
        		autoState = AutoState.DRIVING;
        		break;
        	}
    		/*case LOWERING_INTAKE_ARM:{
    			if(!intakeArmDown.get()){
            		intakeArm.set(0);
            		if(autoLowerSArm){
            			autoState = AutoState.LOWERING_S_ARM;
            			//arm.set(-0.5);
            		}else{
            			autoState = AutoState.DRIVING;
        				setDrive(-.4);
        				autoTimer.reset();
        				autoTimer.start();
            		}
    			}
    			break;
    		}
    		case LOWERING_S_ARM:{
    			if(!armForward.get()){
    				//arm.set(0);
    				autoState = AutoState.DRIVING;
    				setDrive(-.4);
    				autoTimer.reset();
    				autoTimer.start();
    			}
    			break;
    		}*/
			case DRIVING:{
				if(autoTimer.get() >= 7){
    				setDrive(0);
    				autoState = AutoState.FINISHED;
				}
				break;
			}
			case FINISHED:{
				//Lalala done
				break;
			}
		default:
			break;
        }
    }


    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
    	
    	/* Main
    	 * Drive = Joysticks
    	 * all out = left trigger
    	 * intake out = left bumper
    	 * intake in = right bumper
    	 * 
    	 * 
    	 * Partner
    	 * conveyor out = left bumper
    	 * conveyor in = right bumper
    	 * shooter fire = right trigger
    	 * arm = left joystick
    	 * all out = B
    	 * 
    	 */
    	
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
        
        setDrive(leftSpeed, rightSpeed);

        // TODO: Change to toggle for camera light
        if (joysticks[0].getRawButton(Buttons.A)) {
            cameraLight.set(1);
            System.out.println("Light on");
        } else {
            cameraLight.set(0);
            System.out.println("Light off");
        }

        // Run full eject of boulder. Intake, conveyor, and shooter all reverse
        // This overrides all other boulder transport commands
        if (joysticks[0].getRawAxis(Axises.LEFT_TRIGGER) > 0.5 || joysticks[1].getRawButton(Buttons.B)) {
            moveBoulder(Directions.OUT);
        } else {
            // Intake
            // Only one of the bumpers should be pressed. If both are pressed then the motor will stop
            if (joysticks[0].getRawButton(Buttons.LEFT_BUMPER) ^ joysticks[0].getRawButton(Buttons.RIGHT_BUMPER)) {
                if (joysticks[0].getRawButton(Buttons.LEFT_BUMPER)) {
                    moveIntake(Directions.OUT);
                } else if (joysticks[0].getRawButton(Buttons.RIGHT_BUMPER)) {
                    moveIntake(Directions.IN);
                }
            } else {
                moveIntake(Directions.STOP);
            }

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

            double shooterSpeed = shooterEncoder.getRate();
            // Shooter
            if (joysticks[1].getRawAxis(Axises.RIGHT_TRIGGER) > 0.5 && shooterSpeed <= desiredShooterSpeed) {
                moveShooter(Directions.IN);
            } else {
                moveShooter(Directions.STOP);
            }
        }


        // - forward
        // + backward
        // go - if back is pressed
        // go + if forward is pressed

        //double armJoysickPosition = deadband(joysticks[1].getRawAxis(Axises.LEFT_Y));
        //armJoysickPosition *= SmartDashboard.getNumber("Defense arm max speed", 1);

        //if (!armBack.get()) {
            //arm.set(Math.min(0, armJoysickPosition));
        //} else if (!armForward.get()) {
            //arm.set(Math.max(0, armJoysickPosition));
        //} else {
            //arm.set(armJoysickPosition);
        //}

        double intakeArmJoystickPosition = deadband(joysticks[1].getRawAxis(Axises.RIGHT_Y));
//        if (!intakeArmDown.get()) {
//            intakeArm.set(Math.max(0, intakeArmJoystickPosition));
//        } else {
            intakeArm.set(intakeArmJoystickPosition);
//        }

        
        //if (joysticks[1].getRawButton(Buttons.Y)) {
           //lift.set(-1);
        //} else if (joysticks[1].getRawButton(Buttons.A)) {
        	//lift.set(1);
        //} else {
        	//lift.set(0);
        //}

        double shooterSpeed = shooterEncoder.getRate();

        SmartDashboard.putNumber("Shooter speed value", shooterSpeed);

        updateFlagLights(shooterSpeed >= desiredShooterSpeed, !ballLoaded.get());

        updateDSLimitSW();
    }


    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
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


    void updateFlagLights(boolean shooterReady, boolean ballIn) {
        boolean ballLight = ballLightOn;
        boolean shooterLight = shooterLightOn;

        if (ballIn) {
            if (!ballWasIn) {
                ballWasIn = true;
                ballLight = false;
                ballLightTimer.reset();
                ballLightTimer.start();
            } else if (ballLightTimer.hasPeriodPassed(0.2)) {
                ballLight = !ballLight;
            }
        } else {
            ballWasIn = false;
            ballLight = true;
        }

        if (shooterReady) {
            if (!shooterWasReady) {
                shooterWasReady = true;
                shooterLight = false;
                shooterLightTimer.reset();
                shooterLightTimer.start();
            } else if (shooterLightTimer.hasPeriodPassed(0.2)) {
                shooterLight = !shooterLight;
            }
        } else {
            shooterWasReady = false;
            shooterLight = true;
        }

        if (ballLight && shooterLight) {
            flagLightsRelay.set(Relay.Value.kOn);
        } else if (shooterLight) {
            flagLightsRelay.set(Relay.Value.kForward);
        } else if (ballLight) {
            flagLightsRelay.set(Relay.Value.kReverse);
        } else {
            flagLightsRelay.set(Relay.Value.kOff);
        }

        ballLightOn = ballLight;
        shooterLightOn = shooterLight;
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
            intake.set(SmartDashboard.getNumber("Intake forward speed", 1));
            break;
        case OUT:
            intake.set(-SmartDashboard.getNumber("Intake backward speed", 1));
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
            shooter.set(0);
            break;
        case IN:
            shooter.set(SmartDashboard.getNumber("Shooter forward speed", 1));
            break;
        case OUT:
            shooter.set(-SmartDashboard.getNumber("Shooter backward speed", 1));
            break;
        }
    }

    void moveBoulder(Directions boulderDirection) { moveBoulder(boulderDirection, boulderDirection, boulderDirection); }
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
     * Returns the y value of the joystick for the respective motor.
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
     * Determines whether to use the reduced speed mode
     *
     * @return true if reduced speed, false if not
     */
    boolean getSpeedMapping() {
        return joysticks[0].getRawAxis(Axises.LEFT_TRIGGER) > 0.5;
    }


    /**
     * Invert left side so the motor turns in the correct direction. The left
     * and right sides have to be inverted because the motors are mirrored
     */

    void setDrive(double left, double right) {
        SmartDashboard.putNumber("right motor", right);
        SmartDashboard.putNumber("left motor", left);
        leftDrive.set(-left);
        rightDrive.set(right);
    }

    void setDrive(double speed) {
        setDrive(speed, speed);
    }
}
