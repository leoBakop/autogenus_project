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
    hasTheBall= playerID==1;
    onDefence=true;

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
    

    while(true){
      while(hasTheBall){
        attackPlayerOne(); 
      }
      while(!hasTheBall){
        attackPlayerTwo();
      }
    }  
  }

  //=========================================================================================

  
  public void passing(int teammatePos){
    boolean caseThree=false;
    camera.searchForBall();
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
        caseThree=true;
        break;
      default:
      //do nothung if the ball is in front of you
    }
    sendPlanMesagges((byte) 82);
    System.out.println("behind the ball");
    goingBehindTheBall(true, caseThree);
    shoot();

  } 

  private void goingBehindTheBall(boolean shoot, boolean caseThree){
    
    if(caseThree){
      for (int i = 0; i < 4; i++) 
        playMotion(sideStepRightMotion);
    }

    double oldBallDir = getBallDirection();
    double oldBallDist =getBallDistance();
    double a=0.5; 
    boolean ballPrevioslyFount=false;

    while(Math.abs(oldBallDir)>0.2 ){
      if(a<0.0) a=0.0;
      if(a>1.0) a=1.0;

      if(Math.random()<a) playMotion(sideStepRightMotion);
      else playMotion(sideStepLeftMotion);
      searchBall();
      double newBallDir = getBallDirection();
      
      if(Math.abs(newBallDir)<=0.1) break;

      if(newBallDir==camera.UNKNOWN) break;//might means that the ball is on shadow 
      

      if(oldBallDir> 0.0 && newBallDir > oldBallDir){ //it means that the robot is righter than the ideal
        a+=0.25;
      }else if(oldBallDir< 0.0 && newBallDir < oldBallDir){ ////it means that the robot is lefter than the ideal
        a-=0.25;
      }else if(oldBallDir< 0.0 && newBallDir >oldBallDir){ //it means that the robot must go right
        a-=0.25;
      }else if(oldBallDir> 0.0 && newBallDir <oldBallDir){ //it means that the robot must go left
        a+=0.25;
      }else { //do sth random
        playMotion(sideStepLeftMotion);
      }

      System.out.println("a is "+a);
      System.out.println("new dir is "+ newBallDir);

      //new line
      oldBallDir=newBallDir;
    }


    
    if (getBallDirection()!=camera.UNKNOWN){
      while(getBallDistance()>0.2){
        playMotion(forwardsMotion);
        searchBall();
      }
    }else{
      if(shoot){
        System.out.println("ball dir is unknown");
        playMotion(sideStepLeftMotion);
        playMotion(sideStepLeftMotion);
        playMotion(forwardsMotion);
      }
    } 
    

    if(getBallDirection()>-0.25 && shoot){
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

    final int LIMIT=3;
    System.out.println("inside best tactic");

    if(teammates.isEmpty()) {System.out.println("-2"); return -2.0; }    

    if(goalDist<0 && goalDist>-4){ //shoot
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
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1 && Math.abs(opponentsDist.get(i))<9) 
        increaseLinkedList(opponentCounter, 1);
    }

    for(int i=2*super.REGEIONS; i<3*super.REGEIONS; i++){
      if(Math.abs(teammates.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(teammatesDist.get(i))<11)
        increaseLinkedList(teammateCounter, 2);
      if(Math.abs(opponents.get(i)-UNKNOWN_DISTANCE)>0.1&& Math.abs(opponentsDist.get(i))<9)
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
    int maxIndex=-2;
    boolean zeroOpponentsInMax=false;
    int middleIndex=0;
    int tmp=0;
    boolean opponentsZero=false;
    System.out.println("teammate counter");
    printList(teammateCounter);
    System.out.println("opponent counter");
    printList(opponentCounter);

    for(int i=0; i<4; i++){
      tmp=teammateCounter.get(i) - opponentCounter.get(i);
      opponentsZero= (opponentCounter.get(i)==0);

    
      if((tmp>max && opponentsZero) || tmp>=max+2){
        if(tmp-max <4) //teammate is somewhere in the middle tmp<previous_max+4
          middleIndex=i-1;
        else{
          maxIndex=i;
          max=tmp;
        }
        System.out.println("maximum is "+ maxIndex);
        
        
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
    
    super.trackBall();
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

 
  public void attackPlayerOne(){
    
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
        sendPlanMesagges((byte) 82);
        sameDirWithGoal();
        camera.searchForGoal();
        double goalDist =getGoalDistance();
        System.out.println("goal dist is "+ goalDist);

        LinkedList <Boolean> teammatePosition = new LinkedList<Boolean>(); //left = true and right is false
        LinkedList <Boolean> opponentPosition = new LinkedList<Boolean>(); //left = true and right is false
        LinkedList<Double> teammateDir;
        LinkedList <Double> opponentsDir;
        LinkedList<Double> teammateDist;
        LinkedList<Double> opponentDist;
        //right before i search for teammates, everyone should stop walking
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
            sameDirWithGoal();
            goingBehindTheBall(false,false);
            sendPlanMesagges((byte) 83);
            walkWithTheBall();
            //testing code
            //the best part for that code is after shooting 
            //or upper with a chance //it has been placed correctly
            if(opponentsNearBall()){ //playing defence
              System.out.println("playing defence");
              sendPlanMesagges((byte) 81);
              onDefence=true;
              defencePlayerOne();
            }else cameraInitialization();
            //end of testing code

          }else if(bestTeammatePos!=-1){
            System.out.println("best teamate in on the "+bestTeammatePos);
            int integerBestPos=(int)Math.floor(bestTeammatePos);
            if(integerBestPos==0) integerBestPos=1;
            
            passing(integerBestPos);
            sendPlanMesagges((byte) 84);
            hasTheBall=false;
            sameDirWithGoal(); //in order to walk straight
            return;
          }
          

        }else { 
          goingBehindTheBall(true, false);
          shoot();
          if(opponentsNearBall()){ //playing defence
            System.out.println("playing defence");
            sendPlanMesagges((byte) 81);
            onDefence=true;
            defencePlayerOne();
          }else cameraInitialization();
          
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

  public void attackPlayerTwo(){
    step(SIMULATION_STEP);
    
    while(true){
      runStep();
      getUpIfNecessary();
      byte tactic= getIncomingMessage();
      
      while(tactic!=82 && tactic!=81){
        playMotion(forwards50Motion);
        tactic=getIncomingMessage();
      }

      while(tactic==82){
        
        sleepSteps(10); 
        tactic=getIncomingMessage();
      }

      while(tactic==81){
        defence();
        tactic=getIncomingMessage(); 
      }
      while(tactic==83){
        playMotion(forwardsMotion);
        tactic=getIncomingMessage();
      }
      if(tactic==84){
        hasTheBall=true;
        System.out.println("attack player two terminated");
        return;
      }

      
    } 
  }

  public void walkWithTheBall(){
    int steps=30;
    final double HEAD_YAW_MAX = 2.0;
    double yawAngle;
    for(int i=0; i<5; i++){
      playMotion(forwardsMotion);
    }
    //in case to find the ball easily
    camera.selectBottom();
    for (int i = steps - 1; i >= 0; i--) {
      yawAngle = ((double)i / (steps - 1) * 2.0 - 1.0) * HEAD_YAW_MAX;
      headYaw.setPosition(clamp(yawAngle, minHeadYawPosition, maxHeadYawPosition));
      step(SIMULATION_STEP);
    }
  }

  public void sameDirWithGoal(){
    //------------------------------------------------------------------
    //in order to initiallize the camera
    int steps = 30;
    final double HEAD_YAW_MAX = 2.0;
    double yawAngle;

    headPitch.setPosition(0.0);  // horizontal head
    camera.selectTop();  // use top camera

    for (int i = 0; i < steps/2; i++) {
      yawAngle = ((double)i / (steps - 1) * 2.0 - 1.0) * HEAD_YAW_MAX;
      headYaw.setPosition(clamp(yawAngle, minHeadYawPosition, maxHeadYawPosition));
      step(SIMULATION_STEP);
    }
    //---------------------------------------------------------------------------
    playMotion(backwardsMotion); //now added
    System.out.println("inside same dir");
    camera.searchForGoal();
    double goalDir=getGoalDirection();
    System.out.println(goalDir);
    while(Math.abs(goalDir)>=0.30){  

      if(goalDir<-0.6) turnLeft60();
      else if(goalDir<=-0.3) turnLeft40();
      else if(goalDir >0.6) turnRight60();
      else if(goalDir>=0.3)  turnRight40();
      
      camera.searchForGoal();
      goalDir=getGoalDirection();
      System.out.println("goal direction found "+goalDir);
    }
    System.out.println("return same dir with goal");
    playMotion(forwardsMotion);
    return;
  }

  public boolean nearBall(){
    double maxDist=1.2;
    headScan();
    double ballDist=getBallDistance();
    return ballDist<maxDist;
  }

  public void defence(){

    sameDirWithGoal(); //for first time and then just go backwards until a certain spot
    double goalDist=getGoalDistance();
    while(goalDist>=-8){ 
      playMotion(backwardsMotion);
      camera.searchForGoal();
      goalDist=getGoalDistance();
      if(getIncomingMessage()==83) return;
    }
    
  }

  public void defencePlayerOne(){

    double goalDist=0.0;
    while(onDefence){
      if((! opponentsNearBall()) || nearBall()){  //if my oponents are not near the ball or i amm near the ball then attack
        onDefence=true;
        sendPlanMesagges((byte) 83); //inform the teamtes that is time for attack
        return ;
      }
      camera.searchForGoal();
      goalDist=getGoalDistance();
      if(goalDist>-2.4){ 
        playMotion(backwardsMotion); 
        System.out.println("goal dist inside defence player one"+goalDist);
      }else goingBehindTheBall(false, false);
    }
  }
}
