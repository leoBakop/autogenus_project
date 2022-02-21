//-----------------------------------------------------------------------------
//  File:         FieldPlayer.java (to be used in a Webots java controllers)
//  Date:         April 30, 2008
//  Description:  Field player "2", "3" or "4" for "red" or "blue" team
//  Project:      Robotstadium, the online robot soccer competition
//  Author:       Yvan Bourquin - www.cyberbotics.com
//  Changes:      November 4, 2008: Adapted to Webots6
//-----------------------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedList;

import com.cyberbotics.webots.controller.*;

public class FieldPlayer extends Player {

  private Motion backwardsMotion, forwardsMotion, forwards50Motion, turnRight40Motion, turnLeft40Motion;
  private Motion turnRight60Motion, turnLeft60Motion, turnLeft180Motion, sideStepRightMotion, sideStepLeftMotion;
  private Motion shooting;
  private double goalDir = 0.0; // interpolated goal direction (with respect to front direction of robot body)
  private boolean shortDist; //new


  public FieldPlayer(int playerID, int teamID) {
    super(playerID, teamID);
    backwardsMotion     = new Motion("../../motions/Backwards.motion");
    forwardsMotion      = new Motion("../../motions/Forwards.motion");
    forwards50Motion    = new Motion("../../motions/Forwards50.motion");
    turnRight40Motion   = new Motion("../../motions/TurnRight40.motion");
    turnLeft40Motion    = new Motion("../../motions/TurnLeft40.motion");
    turnRight60Motion   = new Motion("../../motions/TurnRight60.motion");
    turnLeft60Motion    = new Motion("../../motions/TurnLeft60.motion");
    turnLeft180Motion   = new Motion("../../motions/TurnLeft180.motion");
    sideStepRightMotion = new Motion("../../motions/SideStepRight.motion");
    sideStepLeftMotion  = new Motion("../../motions/SideStepLeft.motion");
    shooting= new Motion ("../../motions/Shoot.motion");  // new code
    shortDist=false;

    // move arms along the body
    Motor leftShoulderPitch = getMotor("LShoulderPitch");
    leftShoulderPitch.setPosition(1.5);
    Motor rightShoulderPitch = getMotor("RShoulderPitch");
    rightShoulderPitch.setPosition(1.5);
  }

  // normalize angle between -PI and +PI
  private double normalizeAngle(double angle) {
    while (angle > Math.PI) angle -= 2.0 * Math.PI;
    while (angle < -Math.PI) angle += 2.0 * Math.PI;
    return angle;
  }

  // relative body turn
  private void turnBodyRel(double angle) {
    if (angle > 0.7)
      turnRight60();
    else if (angle < -0.7)
      turnLeft60();
    else if (angle > 0.3)
      turnRight40();
    else if (angle < -0.3)
      turnLeft40();
  }

  protected void runStep() {
    super.runStep();
    double dir = camera.getGoalDirectionAngle();
    if (dir != NaoCam.UNKNOWN)
      goalDir = dir - headYawPosition.getValue();
  }

  private void turnRight60() {
    playMotion(turnRight60Motion); // 59.2 degrees
    goalDir = normalizeAngle(goalDir - 1.033);
  }

  private void turnLeft60() {
    playMotion(turnLeft60Motion); // 59.2 degrees
    goalDir = normalizeAngle(goalDir + 1.033);
  }

  private void turnRight40() {
    playMotion(turnRight40Motion); // 39.7 degrees
    goalDir = normalizeAngle(goalDir - 0.693);
  }

  private void turnLeft40() {
    playMotion(turnLeft40Motion); // 39.7 degrees
    goalDir = normalizeAngle(goalDir + 0.693);
  }
  
  private void turnLeft180() {
    playMotion(turnLeft180Motion); // 163.6 degrees
    goalDir = normalizeAngle(goalDir + 2.855);
  }

  //method that perform the same moves as above
  //but without tracking the ball 
  // in order to keep the top camera turned on
  // in order to find teammates


  private void tTurnLeft60(){
    tPlayMotion(turnLeft60Motion);
    goalDir = normalizeAngle(goalDir - 1.033);
  }

  @Override public void run() {
    
    step(SIMULATION_STEP);

    while (true) {
      
      runStep();

      getUpIfNecessary();

      
      int i = 1;

      while (getBallDirection() == NaoCam.UNKNOWN) {
        if(i%3==0){//if you have already searched for the ball three times, take a step back.
          playMotion(backwardsMotion);
          playMotion(backwardsMotion);
          System.out.println(i);
          i++;
        }
        if(shortDist){
          playMotion(backwardsMotion);
          playMotion(backwardsMotion);
          shortDist=false;
        }else{
        System.out.println("searching the ball"); 
        getUpIfNecessary();
        if (getBallDirection() != NaoCam.UNKNOWN) break;
        System.out.println("performing head scan");
        headScan();
        if (getBallDirection() != NaoCam.UNKNOWN) break;
        playMotion(backwardsMotion);
        if (getBallDirection() != NaoCam.UNKNOWN) break;
        headScan();
        if (getBallDirection() != NaoCam.UNKNOWN) break;
        turnLeft180();
        }
        
      }

      double ballDir = getBallDirection();
      double ballDist = getBallDistance();

      


      System.out.println("ball dist: " + ballDist + " ball dir: " + ballDir + " goal dir: " + goalDir);

      if (ballDist < 0.3) {

      LinkedList <Boolean> teammatePosition = new LinkedList<Boolean>(); //left = true and right is false
      LinkedList<Double> teammateDir;
      LinkedList<Double> teammateDist;
      LinkedList<Double> opponentDist;

      if(true){ // for testing search for teammate but then i have to search periodicly
        for(int j=0; i<7; i++){
          System.out.println("searching for teammate");
          if(camera.searchTeammate()){
            if(i<3) {
              teammatePosition.addLast(true);
              System.out.println("teammate was found on the left");
            }
            else if(i>4) {
              System.out.println("teammate was found on the right");
              teammatePosition.addLast(false);
            }
          } 
          
          tTurnLeft60();
          
        }
        teammateDir =getTeammateDirection();
        teammateDist = getTeammateDistance();
        opponentDist=getOpponentDistance();
        camera.selectBottom();
      }

      if(teammateDir!=null && teammateDist!=null) {
        //current section testing
        //find the best tactic for the current state(pass or shoot)
        LinkedList bestTeammate=bestTactic(teammateDir, teammatePosition, opponentDist);
        double bestTeammateDir=(double) bestTeammate.getFirst(); //get the first element of the tuple
        boolean isTeammateLeft=(boolean) bestTeammate.getLast(); //get the second (last) element of the tuple
        if(isTeammateLeft) System.out.println("best teammate is on the left"); 
        else System.out.println("best teammate is on the right");
        //testing passing session
        passing(isTeammateLeft, bestTeammateDir, ballDist);
        //end of current section testing
      }else System.out.println("no teammate was found");



        System.out.println("short distance");
        shortDist=true;
        if (ballDir < -0.15){    //sometimes there is a deadlock between 138 and 142 line
          playMotion(sideStepLeftMotion);
          System.out.println("lagou step left");
        }
        else if (ballDir > 0.15 && Math.random()<0.85){ // we enter this if with 75% propability
          playMotion(sideStepRightMotion);
        }
        else if (goalDir < -0.35)
          turnLeft40();
        else if (goalDir > 0.35)
          turnRight40();
        else {
          System.out.println("try to shoot");
          playMotion(sideStepRightMotion);// my robot is a left footed player so in order to shoot the ball with the left foot
          System.out.println("took a small right step");// it performs a right step
          if(ballDist>0.17)
            playMotion(forwardsMotion); //take a small forward step
          System.out.println("shooting !!!");
          playMotion(shooting);
        }
      }
      else {
        shortDist=false;//new code
        
        double goDir = normalizeAngle(ballDir - goalDir);

        if (goDir < ballDir - 0.5)
          goDir = ballDir - 0.5;
        else if (goDir > ballDir + 0.5)
          goDir = ballDir + 0.5;

        goDir = normalizeAngle(goDir);

        turnBodyRel(goDir);
        if (ballDist < 0.6)
          playMotion(forwardsMotion);
        else
          playMotion(forwards50Motion);
      }
    }
  }

  private void passing(boolean isLeft, double dir, double ballDist){
    System.out.println("trying to pass");
   
    if(isLeft){//teammate is on the left
      System.out.println("teammate is on the left");
      turnBodyRel(dir);
      playMotion(backwardsMotion);
      playMotion(backwardsMotion);
      for(int i=0; i<5; i++){
        playMotion(sideStepRightMotion);
      }

      playMotion(forwardsMotion);
      playMotion(sideStepRightMotion);
      for(int j=0; j<3; j++)
        playMotion(forwardsMotion);
      
      turnBodyRel(dir);
      playMotion(shooting);
    }else if(!isLeft){ //teammate is on the right

      System.out.println("teammate is on the right");
      playMotion(sideStepLeftMotion);
      playMotion(sideStepLeftMotion);
      turnRight60();
      playMotion(backwardsMotion);
      playMotion(backwardsMotion);
      for(int i=0; i<4; i++){
        playMotion(sideStepLeftMotion);
      }
      playMotion(forwardsMotion);
      playMotion(sideStepRightMotion);
      System.out.println("outside right if");
      
      turnBodyRel(-dir);
      if(Math.abs(dir)<0.3){
        playMotion(forwardsMotion);
        playMotion(sideStepRightMotion);
      }else if(Math.abs(dir)>0.3){
        playMotion(sideStepLeftMotion);  
      }
      else if(Math.abs(dir)>0.45){
        playMotion(sideStepLeftMotion);
        playMotion(sideStepLeftMotion);
      }

      playMotion(shooting);

    }
  }

  //returns a tuple with [direction of teammate, is he left or right from the ball handler]
  private LinkedList bestTactic(LinkedList<Double> teammates, LinkedList<Boolean> position,
                               LinkedList<Double> opponents ){
    LinkedList retVal= new LinkedList();
    if(teammates.isEmpty()) return null;

    retVal.addLast(teammates.getFirst());
    retVal.addLast(position.getFirst());
    

    return retVal;
  }
}
