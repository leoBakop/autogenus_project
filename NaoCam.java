//-----------------------------------------------------------------------------
//  File:         NaoCam.java (to be used in a Webots java controllers)
//  Date:         April 30, 2008
//  Description:  Camera with simple blob localization capability
//  Project:      Robotstadium, the online robot soccer competition
//  Author:       Yvan Bourquin - www.cyberbotics.com
//  Changes:      November 4, 2008: Adapted to Webots6
//-----------------------------------------------------------------------------

import java.lang.Math;
import java.util.LinkedList;

import com.cyberbotics.webots.controller.Camera;
import com.cyberbotics.webots.controller.Robot;

public class NaoCam {

  public static final double UNKNOWN = -999.9;
  public static final double OFFSET_ANGLE = 0.6981;  // 40 degrees between cameras axes
  public enum Goal { SKY_BLUE, YELLOW, UNKNOWN_COLOR };

  boolean topSelected = true;
  Camera topCamera;
  Camera bottomCamera;
  int timeStep;
  private Goal goalColor;
  private double fov;
  private int width, height;
  private int[] image;
  private double blobDirectionAngle = UNKNOWN;
  private double blobElevationAngle = UNKNOWN;
  private LinkedList<Double> tDirAngle; //teammate dir angle
  private LinkedList<Double> tElevationAngle;
  private LinkedList<Double> opDirAngle; //opponent dir angle
  private LinkedList<Double> opElevationAngle;


  private double ballDirectionAngle = UNKNOWN;
  private double ballElevationAngle = UNKNOWN;
  private double goalDirectionAngle = UNKNOWN;

  private double goalElevationAngle=UNKNOWN;

  //new code
  private boolean isRed;

  public NaoCam(Player robot, int timeStep) {  //change from Robot to Player
    topCamera = robot.getCamera("CameraTop");
    bottomCamera = robot.getCamera("CameraBottom");
    
    tDirAngle=new LinkedList<Double>();
    tElevationAngle=new LinkedList<Double>();

    opDirAngle=new LinkedList<Double>();
    opElevationAngle=new LinkedList<Double>();

    this.timeStep = timeStep;
    goalColor = Goal.UNKNOWN_COLOR;
    isRed=robot.isRed();
    // start with top camera
    selectTop();
  }
  
  public void setGoalColor(Goal goal) {
    this.goalColor = goal;
  }

  // find a blob whose rgb components match
  private void findColorBlob(int R, int G, int B, int threshold) {

    int x = 0, y = 0;
    int npixels = 0;

    for (int i = 0; i < image.length; i++) {
      int r = Camera.pixelGetRed(image[i]);
      int g = Camera.pixelGetGreen(image[i]);
      int b = Camera.pixelGetBlue(image[i]);

      if (Math.abs(r - R) + Math.abs(g - G) + Math.abs(b - B) < threshold) {
        x += i % width;
        y += i / width;
        npixels++;
      }
    }

    if (npixels > 0) {
      blobDirectionAngle = ((double)x / npixels / width - 0.5) * fov;
      blobElevationAngle = -((double)y / npixels / height - 0.5) * fov;
    }
    else {
      blobDirectionAngle = UNKNOWN;
      blobElevationAngle = UNKNOWN;
    }
  }

  // analyse image and find blobs
  public void processImage() {

    if (topSelected)
      image = topCamera.getImage();
    else
      image = bottomCamera.getImage();
    
    // find orange ball
    findColorBlob(240, 140, 50, 60);
    ballDirectionAngle = blobDirectionAngle;
    ballElevationAngle = blobElevationAngle;

    
    

    // find goal
    switch (goalColor) {
    case SKY_BLUE:
      findColorBlob(30, 200, 200, 60);
      break;
    case YELLOW:
      findColorBlob(140, 140, 15, 60);
      break;
    default:
      goalDirectionAngle = UNKNOWN;
    }

    goalDirectionAngle = blobDirectionAngle;
    goalElevationAngle =blobElevationAngle;
  }


  public void searchForGoal(){
    selectTop();
    image = topCamera.getImage();
    // find goal
    switch (goalColor) {
      case SKY_BLUE:
        findColorBlob(30, 200, 200, 60);
        break;
      case YELLOW:
        findColorBlob(140, 140, 15, 60);
        break;
      default:
        goalDirectionAngle = UNKNOWN;
      }

    goalDirectionAngle = blobDirectionAngle;
    goalElevationAngle =blobElevationAngle;
    selectBottom();
  }

  //retvals 0 -> no opponent and no teammate was found
  //retvals 1 -> no opponent was found  and  teammate was found
  //retvals 2 -> opponent was found and no teammate was found
  //retvals 3 ->  opponent and  teammate was found

  //blobDirectionAngle!=UNKNOWN && blobElevationAngle<UNKNOWN/2
  //the above condition exists in order to avoid the long distances teammates and opponents
  //in black we set small threshold 
  public int searchTeammate(){

    if (topSelected)
      image = topCamera.getImage();
    else
      image = bottomCamera.getImage();

    int retVal=0;
    

    //oponent detection
    if(isRed)findColorBlob(40, 178, 220, 40); //blue
    else findColorBlob(0,0,0, 20); //black

    if(blobDirectionAngle!=UNKNOWN ){
      System.out.println("opponent dir "+ blobDirectionAngle);
      opDirAngle.addLast( blobDirectionAngle);
      opElevationAngle.addLast(blobElevationAngle);
      retVal=2;
    }else{
      opDirAngle.addLast(0.0);
      opElevationAngle.addLast(0.0);
    }
    
    //teammate search
    if(retVal==2){ //if  it has already been found an opponent reduce the sensitivity of black
      if(isRed) findColorBlob(0,0,0, 5); //black
      else findColorBlob(51, 57, 240, 30);  
    }else{
      if(isRed) findColorBlob(0,0,0, 15); //black
      else findColorBlob(51, 57, 240, 30);  
    }
    

    if(blobDirectionAngle==UNKNOWN) findColorBlob(102,13,13, 60); //if my teammate is not black, try red
   
    if(blobDirectionAngle!=UNKNOWN){
      retVal++;
      tDirAngle.addLast(blobDirectionAngle);
      tElevationAngle.addLast(blobElevationAngle);
      System.out.println("teammate dir "+ blobDirectionAngle);
    }else{
      tDirAngle.addLast(0.0);
      tElevationAngle.addLast(0.0);
    }
    
    
    
    return retVal;
    
  }

  // all direction and elevation angles are indicated in radians
  // with respect to the camera focal (or normal) line
  // a direction angle will not exceed +/- half the field of view
  // a positive direction is towards the right of the camera image
  // a positive elevation is towards the top of the camera image

  //so negative means left
  public double getBallDirectionAngle() {
    return ballDirectionAngle;
  }

  public double getBallElevationAngle() {
    return ballElevationAngle;
  }

  public double getGoalDirectionAngle() {
    return goalDirectionAngle;
  }

  public double getGoalElevationAngle(){
    return goalElevationAngle;
  }

  public LinkedList<Double> getTeammateDirAngle(){
    return tDirAngle;
  }
  public LinkedList<Double> getTeammateElevationAngle(){
    return tElevationAngle;
  }
  public LinkedList<Double> getOpponentDirAngle(){
    return opDirAngle;
  }
  public LinkedList<Double> getOpponentElevationAngle(){
    return opElevationAngle;
  }
  
  public void selectTop() {
    bottomCamera.disable(); 
    topCamera.enable(timeStep);
    fov = topCamera.getFov();
    width = topCamera.getWidth();
    height = topCamera.getHeight();
    topSelected = true;
  }

  public void selectBottom() {
    topCamera.disable(); 
    bottomCamera.enable(timeStep);
    fov = bottomCamera.getFov();
    width = bottomCamera.getWidth();
    height = bottomCamera.getHeight();
    topSelected = false;
  }
  
  public double getOffsetAngle() {
    if (topSelected)
      return 0.0;
    else
      return OFFSET_ANGLE;
  }
}
