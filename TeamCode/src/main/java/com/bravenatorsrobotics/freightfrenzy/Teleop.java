package com.bravenatorsrobotics.freightfrenzy;

import com.bravenatorsrobotics.common.core.FtcGamePad;
import com.bravenatorsrobotics.common.drive.MecanumDrive;
import com.bravenatorsrobotics.common.operation.TeleopMode;
import com.bravenatorsrobotics.common.utils.PowerScale;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

@TeleOp(name="Teleop")
public class Teleop extends TeleopMode<MecanumDrive> {

    // TODO: Replace with tested values
    private static final int LIFT_STAGE_1 = 45;
    private static final int LIFT_STAGE_2 = 185;
    private static final int LIFT_STAGE_3 = 357;

    private static final double LIFT_POWER = 0.50;

    private static final double CUP_OBJECT_THRESHOLD_CM = 6.0; // CM
    private static final double REDUCE_SPEED_MULTIPLIER = 0.25;

    private static final double CUP_DUMPED_POSITION = 0;
    private static final double CUP_TILTED_POSITION = 0.6;
    private static final double CUP_DOWN_POSITION = 1;

    private static final double INTAKE_SPEED = 0.5;

    private Config config;

    private DcMotorEx lift;
    private DcMotorEx intake;
    private DcMotorEx turnTableSpinner;

    private Servo cupServo;
    private TouchSensor liftTouchSensor;
    private RevColorSensorV3 cupDistanceSensor;

    private boolean shouldOverrideSpeedReduction = false;
    private boolean shouldReduceSpeed = false;
    private boolean shouldReverse = false;

    private boolean shouldZeroLiftEncoder = false;

    private final PowerScale drivePowerScale = new PowerScale(this, 1.0 / 0.25);
    private double currentV = 0.0;
    private double currentH = 0.0;
    private double currentR = 0.0;

    private boolean objectInCupToggle = false;

    private double turnTablePower = 1;

    // Create TeleopMode with specified specifications
    public Teleop() { super(new Specifications()); }

    @Override
    public void OnInitialize() {
        PrintControls();

        config = new Config(hardwareMap.appContext);

        // Reverse the turn table power if on red alliance
        if(config.allianceColor == Config.AllianceColor.BLUE)
            turnTablePower = -turnTablePower;

        lift = robot.GetMotor("lift", false);
        lift.setTargetPositionTolerance(1);
        lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        intake = robot.GetMotor("intake", true);
        turnTableSpinner = robot.GetMotor("turnTable", false);

        cupServo = hardwareMap.servo.get("cupServo");

        liftTouchSensor = hardwareMap.touchSensor.get("liftTouchSensor");

        cupDistanceSensor = hardwareMap.get(RevColorSensorV3.class, "cupDistanceSensor");
    }

    private void InitializeServos() {
        cupServo.setPosition(1);
    }

    private void ZeroLift() {
        if(!liftTouchSensor.isPressed())
            lift.setPower(-0.1);

        while(!liftTouchSensor.isPressed()) {
            if(!opModeIsActive()) break;
        }

        lift.setPower(0);
        lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    @Override
    public void OnStart() {
        telemetry.clearAll();

        ZeroLift();
    }

    @Override
    public void OnUpdate() {

        if(shouldZeroLiftEncoder && liftTouchSensor.isPressed()) {
            lift.setPower(0);
            lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            shouldZeroLiftEncoder = false;
        }

        // Detect Cup
        if(IsObjectInCup() && !objectInCupToggle) {
            objectInCupToggle = true;
            cupServo.setPosition(CUP_TILTED_POSITION);
        } else if(objectInCupToggle && !IsObjectInCup()) {
            objectInCupToggle = false;
        }

        HandleGamePadDrive();

        // Log Important Information
        telemetry.addData("Is Lift Button Pressed", liftTouchSensor.isPressed());
        telemetry.addData("Is Reversed", shouldReverse);
        telemetry.addData("Should Reduce Mode", shouldReduceSpeed);
        telemetry.update();
    }

    @Override
    public void OnStop() {
        robot.Stop();
    }

    private void HandleGamePadDrive() {
        double v = Math.pow(gamepad1.left_stick_y, 3);
        double h = Math.pow(gamepad1.left_stick_x, 3) - Math.pow(driverGamePad.getLeftTrigger(), 3)
                + Math.pow(driverGamePad.getRightTrigger(), 3);
        double r = -Math.pow(gamepad1.right_stick_x, 3);

        if(shouldReverse) {
            v = -v;
            h = -h;
        }

        if(shouldReduceSpeed && !shouldOverrideSpeedReduction) {
            v *= REDUCE_SPEED_MULTIPLIER;
            h *= REDUCE_SPEED_MULTIPLIER;
            r *= REDUCE_SPEED_MULTIPLIER;
        }
//
//        this.currentV = this.drivePowerScale.GetPower(v, this.currentV);
//        this.currentH = this.drivePowerScale.GetPower(h, this.currentH);
//        this.currentR = this.drivePowerScale.GetPower(r, this.currentR);

        super.robot.drive.Drive(v, h, r);
    }

    @Override
    protected void OnDriverGamePadChange(FtcGamePad gamePad, int button, boolean pressed) {
        switch (button) {
            case FtcGamePad.GAMEPAD_BACK:
                if(pressed) shouldOverrideSpeedReduction = !shouldOverrideSpeedReduction;
                break;

            case FtcGamePad.GAMEPAD_LBUMPER: // Slow Down
                if(pressed)
                    shouldReduceSpeed = !shouldReduceSpeed;
                break;

            case FtcGamePad.GAMEPAD_RBUMPER: // Reverse
                if(pressed)
                    shouldReverse = !shouldReverse;
                break;
        }
    }

    @Override
    protected void OnOperatorGamePadChange(FtcGamePad gamePad, int button, boolean pressed) {
        switch (button) {

            // Turntable Direction Override
            case FtcGamePad.GAMEPAD_BACK:
                if(pressed) {
                    turnTablePower = -turnTablePower;
                }

                break;

            // Turntable Spinner
            case FtcGamePad.GAMEPAD_X:

                if(pressed) {
                    turnTableSpinner.setPower(turnTablePower);
                } else {
                    turnTableSpinner.setPower(0);
                }

                break;

            // Intake
            case FtcGamePad.GAMEPAD_A:
                if(pressed) {
                    intake.setPower(INTAKE_SPEED);
                } else {
                    intake.setPower(0);
                }

                break;

            // TODO: Make sure this doesn't fight with GAMEPAD_A
            // Reverse Intake
            case FtcGamePad.GAMEPAD_Y:

                if(pressed) {
                    intake.setPower(-INTAKE_SPEED);
                } else {
                    intake.setPower(0);
                }

                break;

            // Toggle Cup
            case FtcGamePad.GAMEPAD_B:

                if(pressed) {
                    cupServo.setPosition(CUP_DUMPED_POSITION);
                } else {
                    cupServo.setPosition(CUP_DOWN_POSITION);
                }

                break;

            // Automatic Lift Down
            case FtcGamePad.GAMEPAD_DPAD_DOWN:
                if(pressed) {
                    lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    lift.setPower(-LIFT_POWER);
                    shouldZeroLiftEncoder = true;
                }

                break;

            // Automatic Lift Position 1
            case FtcGamePad.GAMEPAD_DPAD_LEFT:
                if(pressed) {
                    lift.setTargetPosition(LIFT_STAGE_1);
                    lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    lift.setPower(LIFT_POWER);
                }

                break;

            // Automatic Lift Position 2
            case FtcGamePad.GAMEPAD_DPAD_UP:
                if(pressed) {
                    lift.setTargetPosition(LIFT_STAGE_2);
                    lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    lift.setPower(LIFT_POWER);
                }

                break;

            // Automatic Lift Position 3
            case FtcGamePad.GAMEPAD_DPAD_RIGHT:
                if(pressed) {
                    lift.setTargetPosition(LIFT_STAGE_3);
                    lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    lift.setPower(LIFT_POWER);
                }

                break;
        }
    }

    private boolean IsObjectInCup() {
        double distance = cupDistanceSensor.getDistance(DistanceUnit.CM);
        return (distance < CUP_OBJECT_THRESHOLD_CM) || (cupDistanceSensor.getLightDetected() > 0.1);
    }

    private void PrintControls() {
        telemetry.log().add("DRIVER CONTROLS");
        telemetry.log().add("(BACK) Emergency Movement Override");
        telemetry.log().add("");

        telemetry.log().add("OPERATOR CONTROLS");
        telemetry.log().add("(BACK) Reverse Turn-Table Direction");
        telemetry.log().add("(X) Turn-Table");
        telemetry.log().add("(A) Intake");
        telemetry.log().add("(Y) Reverse Intake");
        telemetry.log().add("(B) Toggle Cup Position");
        telemetry.log().add("(D-Pad Down) Lift Down");
        telemetry.log().add("(D-Pad Left) Lift First Position");
        telemetry.log().add("(D-Pad Up) Lift Second Position");
        telemetry.log().add("(D-Pad Right) Lift Third Position");

        telemetry.update();
    }

}
