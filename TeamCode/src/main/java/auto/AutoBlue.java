package auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;


import autofunctions.Path;
import autofunctions.RobotFunctions;
import autofunctions.TerraCV;
import global.TerraBot;
import util.CodeSeg;
import util.Rect;
import autofunctions.TerraCV.RingNum;

@Autonomous(name = "AutoBlue", group = "Auto")
public class AutoBlue extends LinearOpMode {
    TerraBot bot = new TerraBot();
    RobotFunctions rf = new RobotFunctions();
    Path path = new Path(0,0,0);

    final double shootSpeed = 1;

    double vs = 0;

    @Override
    public void runOpMode() {
        initialize();
        //rf.generateRandomIM();
        while (!isStarted() && !bot.isDoneResettingArm()) {
            bot.resetArm();
        }
        rf.telemetryText("done initializing");
        rf.scanRings();
        waitForStart();
        bot.startOdoThreadAuto(this);

        path.addWaypoint(-30,50,0);
        path.addRF(rf.wobbleArm(90,1), rf.toggleOuttake(bot));
        path.addWaypoint(10, 40, 0);
        path.addWaypoint(50,48,0);
        powerShot();


        if(rf.ringnum.equals(RingNum.ZERO)) {
            path.addWaypoint(-42, 5, 0);
            path.addSetpoint(-12,10,-103);
            dropWobble();
            path.addWaypoint(40, -15, 10);
        }else if(rf.ringnum.equals(RingNum.ONE)){
            path.addWaypoint(-20, 20, 0);
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            path.addRF(rf.changeAcc(3, 3, 5, path));
            path.addSetpoint(-18,20,-168);
            dropWobble();
            path.addWaypoint(30, -40, 75);
        }else if(rf.ringnum.equals(RingNum.FOUR)){
            path.addWaypoint(-40, 100, -73);
            path.addSetpoint(-12,20,-30);
            dropWobble();
            path.addWaypoint(43, -122, 10);
        }
        path.addSetpoint(3,-35,105);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        path.addRF(rf.changeAcc(1, 1, 1, path));
        path.addSetpoint(-8,-15,0);
        path.addRF(rf.grab(1));
        path.addStop(0.3);
        path.addRF(rf.wobbleArm(100, 1));
        path.addStop(0.3);
        path.addWaypoint(5, 50, 0);
        path.addWaypoint(-25, 0, -90);

//        if(rf.ringnum.equals(RingNum.ZERO)) {
//            path.addWaypoint(-10,10,-25);
//            path.addSetpoint(-5,5,0);
//            dropWobble();
//            path.addWaypoint(20,50,0);
//        }else if(rf.ringnum.equals(RingNum.ONE)){
//            path.addWaypoint(0, 20, -70);
//            path.addSetpoint(0,20,-20);
//            dropWobble();
//            path.addWaypoint(0,20,0);
//        }else if(rf.ringnum.equals(RingNum.FOUR)){
//            path.addWaypoint(-15, 100, -25);
//            path.addSetpoint(0,20,0);
//            dropWobble();
//            path.addWaypoint(0,-70,0);
//        }

        if(rf.ringnum.equals(RingNum.ZERO)) {
            path.addWaypoint(-10,10,-25);
            path.addSetpoint(-5,5,0);
            dropWobble();
            path.addWaypoint(20,50,0);
        }else if(rf.ringnum.equals(RingNum.ONE)){
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            path.addRF(rf.lift(0), rf.shootControl(1), rf.changeAcc(3, 3, 5, path));
            path.addWaypoint(0, 20, -70);
            path.addSetpoint(0,20,-20);
            path.addRF(rf.intake(1), rf.changeOuttakePow(1, vs, path), rf.toggleOuttake(bot));
            path.addWaypoint(0.5,0, 0);
            dropWobble();
            path.addWaypoint(-20,-20,0);
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            path.addRF(rf.changeAcc(1, 1, 1, path));
            path.addSetpoint(0,-30,0);
            path.addWaypoint(-10, 35, 50);
            path.addRF(rf.lift(1));
            path.addStop(0.3);
            path.addSetpoint(-5, 10, 130);
            path.addRF(rf.shootControl(3));
            path.addWaypoint(0,20,0);


        }else if(rf.ringnum.equals(RingNum.FOUR)){
            path.addWaypoint(-15, 100, -25);
            path.addSetpoint(0,20,0);
            dropWobble();
            path.addWaypoint(0,-70,0);
        }


        path.addRF(rf.wobbleArm(10,1), rf.turnArm(0.25));
        path.addStop(2);


        path.start(bot, this);
        bot.stopOdoThreadAuto();

    }
    private void initialize(){
        bot.grabStart = 0.45;
        bot.outrController.acc = 1000;
        bot.outlController.acc = 1000;
        bot.init(hardwareMap);
        rf.init(bot, this);
        bot.lift(bot.liftControl.getPos(1));
        bot.shoot(bot.shootControlR.getPos(2), bot.shootControlL.getPos(2));
        vs = bot.getVoltageScale();
        path.shootSpeed = bot.powerShotSpeed;
        bot.outrController.setStartPow(bot.outtakeStartR*bot.powerShotSpeed * vs);
        bot.outlController.setStartPow(bot.outtakeStartL*bot.powerShotSpeed * vs);
    }
    private void dropWobble(){
        path.addRF(rf.turnArm(0.68), rf.wobbleArm(180,1));
        path.addStop(1);
        path.addRF(rf.grab(0));
        path.addStop(0.5);
        path.addWaypoint(15,-15,0);
        path.addRF(rf.wobbleArm(190,1));
    }

    private void powerShot(){
        path.addSetpoint(13, 10, 0);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        path.addRF(rf.changeAcc(1, 1, 0.5, path), rf.updateXWithDis(60));
        path.addStop(1);
        path.addSetpoint(0,0,0);

        path.addRF(rf.shootControl(3));
        path.addStop(0.3);
        path.addRF(rf.shootControl(2));
        path.addStop(0.3);

        path.addSetpoint(0,0,-6);

        path.addRF(rf.shootControl(3));
        path.addStop(0.3);
        path.addRF(rf.shootControl(2));
        path.addStop(0.3);

        path.addSetpoint(0,0,-6);

        path.addRF(rf.shootControl(3));
        path.addStop(0.3);
        path.addRF(rf.shootControl(2));
        path.addStop(0.3);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        path.addRF(rf.toggleOuttake(bot), rf.changeAcc(1, 1, 1, path));






    }
}
