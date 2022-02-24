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
import java.util.Random;

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
        super.searchForPlayers();

        teammateDist=getTeammateDistance();
        teammateDir =getTeammateDirection();  //we know that the first Size%Position is right righ
        opponentDist=getOpponentDistance();  // the second size%position in right left etc
        opponentsDir=getOpponentDirection();
    
        //alse distances are getting bigger when sth is further
        printList(teammateDist);
        System.out.println("=================================");
        printList(opponentDist);

        if(teammateDir!=null) {

          //current section testing
          //find the best tactic for the current state(pass or shoot)
          double bestTeammatePos=bestTactic(teammateDir, opponentsDir,
                                              teammateDist,opponentDist, goalDist);
          
          System.out.println("best tactic return "+ bestTeammatePos);
          if(bestTeammatePos==-2.0){
            System.out.println("walking with the ball");
            playMotion(forwardsMotion); //walk with the ball
            playMotion(forwardsMotion); //walk with the ball
          }else{
            System.out.println("best teamate in on the "+bestTeammatePos);
            int integerBestPos=(int)Math.floor(bestTeammatePos);
            if(integerBestPos==0) integerBestPos=1;
            //testing passing session
            //passing(bestTeammatePos, bestTeammateDir, ballDist, goalDist);
            passing(integerBestPos);
            //end of current section testing
            
          }
          

        }else { 
          goingBehindTheBall();
          shoot();
          /* System.out.println("no teammate was found");
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
          } */
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

  
  public void passing(int teammatePos){
    camera.searchForBall();
    // -0.1< best ball direction for shooting<0.1 
    
    
    playMotion(backwardsMotion);
    switch(teammatePos){
      case 0: 
        System.out.println("case 0");
        turnRight60();
        break;
      case 1:
      System.out.println("case 1");
        turnRight40();
        break;
      case 2:
        System.out.println("case 2");
        turnLeft40();
        break;
      case 3:
        System.out.println("case 3");
        turnLeft60();
        break;
      default:
      //do nothung if the ball is in front of you
    }
    
    System.out.println("behind the ball");
    goingBehindTheBall();
    shoot();

  } 

  private void goingBehindTheBall(){
    double oldBallDir = getBallDirection();
    double oldBallDist =getBallDistance();
    double a=0.5;

    while(Math.abs(oldBallDir)>0.12 ){
      if(a<0.0) a=0.0;
      if(a>1.0) a=1.0;

      if(Math.random()<a) playMotion(sideStepRightMotion);
      else playMotion(sideStepLeftMotion);
      camera.searchForBall();
      double newBallDir = getBallDirection();
      System.out.println("a is "+a);
      System.out.println("new bal direction is "+ newBallDir);
      if(newBallDir==camera.UNKNOWN) break; //might means that the ball is on shadow
      if(Math.abs(newBallDir)<=0.1) break;
      if(oldBallDir> 0.0 && newBallDir > oldBallDir){ //it means that the robot is righter than the ideal
        a+=0.1;
      }else if(oldBallDir< 0.0 && newBallDir < oldBallDir){ ////it means that the robot is lefter than the ideal
        a-=0.1;
      }else if(oldBallDir< 0.0 && newBallDir >oldBallDir){ //it means that the robot must go right
        a-=0.1;
      }else if(oldBallDir> 0.0 && newBallDir <oldBallDir){ //it means that the robot must go left
        a+=0.1;
      }else { //do sth random
        playMotion(sideStepLeftMotion);
      }
    }
    if (getBallDirection()!=camera.UNKNOWN){
      while(getBallDistance()>0.17){
        playMotion(forwardsMotion);
        camera.searchForBall();
      }
    }
    

    if(getBallDirection()>0){
      playMotion(sideStepRightMotion);
      System.out.println("take a small right step");
    }

    return;
  }
  //returns -2 in case of errr
  //-1 in vase of straight shoot
  //0 in case of right right pass
  //1 in case of right left pass etc
  private double bestTactic(LinkedList<Double> teammates, LinkedList<Double> opponents,
                                LinkedList<Double> teammatesDist, LinkedList<Double> opponentsDist,
                              double goalDist){

    final int LIMIT=4;
    System.out.println("inside best tactic");

    if(teammates.isEmpty()) {System.out.println("-2"); return -2.0; }
    
    

    if(goalDist>0.27){ //shoot
      System.out.println("shooting");
      return -1.0;
    }



    if(opponents == null ) return -2.0;
    if(opponents.isEmpty()) return -2.0;

    LinkedList<Boolean> possibleBestTeammates=new LinkedList<Boolean>();

    LinkedList<Integer> teammateCounter=new LinkedList<Integer>();
    LinkedList <Integer> opponentCounter=new LinkedList<Integer>();

    for(int i=0; i<4; i++){
      teammateCounter.addLast(0);
      opponentCounter.addLast(0);
    }
    
    //priority on shooting in front
    for(int i=super.REGEIONS; i<2*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(teammatesDist.get(i))<11) 
        increaseLinkedList(teammateCounter, 1);
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<11) 
        increaseLinkedList(opponentCounter, 1);
    }

    for(int i=2*super.REGEIONS; i<3*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(teammatesDist.get(i))<11)
        increaseLinkedList(teammateCounter, 2);
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(opponentsDist.get(i))<11)
        increaseLinkedList(opponentCounter, 2);
    }

    for(int i=0; i<super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(teammatesDist.get(i))<15) 
        increaseLinkedList(teammateCounter, 0);
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<15)
        increaseLinkedList(opponentCounter, 0);
    }

    for(int i=3*super.REGEIONS; i<4*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(teammatesDist.get(i))<15) 
        increaseLinkedList(teammateCounter, 3);
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<15) 
        increaseLinkedList(opponentCounter, 3);
    }

    int max=-1;
    int maxIndex=-1;
    int middleIndex=0;
    int tmp=0;
    System.out.println("teammate counter");
    printList(teammateCounter);
    System.out.println("opponent counter");
    printList(opponentCounter);
    for(int i=0; i<4; i++){
      tmp=Math.max(max, teammateCounter.get(i) - opponentCounter.get(i));
      if(tmp>max){
        System.out.println("maximum is "+ maxIndex);
        maxIndex=i;
        max=tmp;
        if(tmp-max < 4) //teammate is somewhere in the middle
          middleIndex=i-1;
      }
    }
    System.out.println("maximum is "+max);
    System.out.println("best teammate ret val "+(double)(middleIndex+(maxIndex-middleIndex)/2));
    if(max>LIMIT){ 
      if(middleIndex+1==maxIndex)
        return (double) middleIndex+ (maxIndex-middleIndex)/2;
      return maxIndex;
    }else return -2.0;
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

  public LinkedList increaseLinkedList(LinkedList<Integer> list, int offset){
    if(list==null || list.size()<offset+1 || offset < 0) return null;  
    int tmp= list.get(offset);
      list.remove(offset);
      list.add(offset, tmp+1);
      return list;
  }

 
}
