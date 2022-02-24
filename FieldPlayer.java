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


  private final double UNKNOWN_DISTANCE=-239004;

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
  private void tTurnLeft40(){
    tPlayMotion(turnLeft40Motion);
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
      
        camera.searchForGoal();
        double goalDist =getGoalDistance();

        LinkedList <Boolean> teammatePosition = new LinkedList<Boolean>(); //left = true and right is false
        LinkedList <Boolean> opponentPosition = new LinkedList<Boolean>(); //left = true and right is false
        LinkedList<Double> teammateDir;
        LinkedList <Double> opponentsDir;
        LinkedList<Double> teammateDist;
        LinkedList<Double> opponentDist;

        if(true){ // for testing search for teammate but then i have to search periodicly

          super.searchForPlayers();

          teammateDist=getTeammateDistance();
          teammateDir =getTeammateDirection();  //we know that the first Size%Position is right righ
          opponentDist=getOpponentDistance();  // the second size%position in right left etc
          opponentsDir=getOpponentDirection();
      
          //alse distances are getting bigger when sth is further
        }
        

        printList(teammateDist);
        System.out.println("=================================");
        printList(opponentDist);

        if(teammateDir!=null) {

          //current section testing
          //find the best tactic for the current state(pass or shoot)
          LinkedList bestTeammate=bestTactic(teammateDir, opponentsDir,
                                              teammateDist,opponentDist, goalDist);
          
          
          if(bestTeammate==null){
            System.out.println("problem");
          }
          printList(bestTeammate);
          System.out.println("is best teammate empty "+bestTeammate.isEmpty());
          if(bestTeammate==null ||bestTeammate.isEmpty())  shoot();
          else{
            double bestTeammateDir=(double) bestTeammate.getFirst(); //get the first element of the tuple
            int bestTeammatePos=(int) bestTeammate.getLast(); //get the second (last) element of the tuple
            System.out.println("best Teammate dir "+ bestTeammateDir);
            System.out.println("is teammate left "+ bestTeammatePos);
            
            if(bestTeammateDir== (double)0.0 && (bestTeammatePos==1 || bestTeammatePos==2)){
              System.out.println("shooting in front");
              System.out.println("beacuse we either find an alone teammmate in front or we are near the goal");
              shoot();
            }else{ 
              //testing passing session
              //passing(bestTeammatePos, bestTeammateDir, ballDist, goalDist);
              passing(bestTeammatePos, bestTeammateDir);
              //end of current section testing
            }
          }
          

        }else {
          System.out.println("no teammate was found");
          System.out.println("short distance");
          shortDist=true;
          if (ballDir < -0.15){    
            playMotion(sideStepLeftMotion);
            System.out.println("lagou step left");
          }
          else if (goalDir < -0.35)
            turnLeft40();
          else if (goalDir > 0.35)
            turnRight40();
          else {
            shoot();
          }
        }
      }else {
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

  //=========================================================================================


  private void passing(int position, double dir){
    camera.selectBottom();

    switch(position){
      case 0: 
        turnRight60();
        break;
      case 3:
        turnLeft60();
        break;
      default:
      //do nothung if the ball is in front of you
    }

    dir=normalizeAngle(dir);
    turnBodyRel(dir);
    for(int i=0; i<4; i++){
      playMotion(sideStepRightMotion);
    }
    shoot();
  }

/*   private void passing(int isLeft, double dir, double ballDist, double goalDist){
    System.out.println("trying to pass");
   
    if(isLeft==3){//teammate is on the left
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
    }else if(isLeft==0){ //teammate is on the right

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
  } */

  //returns a tuple with [direction of teammate, is he left or right from the ball handler]
  private LinkedList bestTactic(LinkedList<Double> teammates, LinkedList<Double> opponents,
                                LinkedList<Double> teammatesDist, LinkedList<Double> opponentsDist,
                              double goalDist){

    final int LIMIT=4;
    System.out.println("inside best tactic");
    LinkedList retVal= new LinkedList();
    if(teammates.isEmpty()) {System.out.println("-1"); return null; }
    
    

    if(goalDist>0.27){ //shoot
      System.out.println("shooting");
      retVal.addFirst(0.0);
      retVal.addLast(1);
      return retVal;
    }



    if(opponents == null ) return null;
    if(opponents.isEmpty()) return null;

    LinkedList<Boolean> possibleBestTeammates=new LinkedList<Boolean>();
    boolean isTeammate=false;
    boolean isOpponent=false;
    int teammateCounter=0;
    int opponentCounter=0;
    //priority on shooting in front
    for(int i=super.REGEIONS; i<2*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(teammatesDist.get(i))<11) teammateCounter++;
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<11) opponentCounter ++;
    }

    if(opponentCounter>LIMIT) isOpponent=true;
    if(teammateCounter>LIMIT) isTeammate=true;
    if(isTeammate && !isOpponent) {
      System.out.println("1");
      retVal.addFirst(teammates.get(3*super.REGEIONS/2));
      retVal.addLast(1);
      return retVal;
    }

    isOpponent=false;
    isTeammate=false;
    teammateCounter=0;
    opponentCounter=0;

    for(int i=2*super.REGEIONS; i<3*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(teammatesDist.get(i))<11) teammateCounter++;
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(opponentsDist.get(i))<11) opponentCounter ++;
    }

    if(opponentCounter>LIMIT) isOpponent=true;
    if(teammateCounter>LIMIT) isTeammate=true;
    if(isTeammate && !isOpponent) {
      System.out.println("2");
      retVal.addFirst(teammates.get(5*super.REGEIONS/2));
      retVal.addLast(2);
      return retVal;
    }

    isOpponent=false;
    isTeammate=false;
    teammateCounter=0;
    opponentCounter=0;


    for(int i=0; i<super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(teammatesDist.get(i))<15) teammateCounter++;
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<15) opponentCounter ++;
    }

    if(opponentCounter>LIMIT) isOpponent=true;
    if(teammateCounter>LIMIT) isTeammate=true;
    if(isTeammate && !isOpponent) {
      System.out.println("0");
      retVal.addFirst(teammates.get(super.REGEIONS/2));
      retVal.addLast(1);
      return retVal;
    }

    isOpponent=false;
    isTeammate=false;
    teammateCounter=0;
    opponentCounter=0;
    
   
    for(int i=3*super.REGEIONS; i<4*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(teammatesDist.get(i))<15) teammateCounter++;
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<15) opponentCounter ++;
    }

    if(opponentCounter>LIMIT) isOpponent=true;
    if(teammateCounter>LIMIT) isTeammate=true;
    if(isTeammate && !isOpponent) {
      System.out.println("3");
      retVal.addFirst(teammates.get(7*super.REGEIONS/2));
      retVal.addLast(1);
      return retVal;
    }

    isOpponent=false;
    isTeammate=false;
    teammateCounter=0;
    opponentCounter=0;
    return retVal;
  }

  public void shoot(){
    System.out.println("tracking ball");
    super.trackBall();
    System.out.println("end of tracking");
    System.out.println("shooting");
    playMotion(shooting);
  }

  private LinkedList<Double> removeFromList(LinkedList<Double> retVal, LinkedList<Integer> offsets){
    if(offsets.size()>retVal.size()) return null;

    
    for(int i=0; i<offsets.size(); i++){
      System.out.println(offsets.get(i) - i);
      //retVal.remove(offsets.get(i)-i); //ex get(2) -2 because you have already remove 2 elements
    }

    return retVal;
  }


  private void printList(LinkedList list){
    for(int i=0; i<list.size(); i++){
      if(i==15 || i==30 || i==0 || i==45) System.out.println("=============");
      System.out.println(i + " "+list.get(i));
    }
  }

  

}
