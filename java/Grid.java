import java.io.*;
import java.lang.Math.*;
import java.util.*;

public class Grid{
  private int numNodes;
  private int numNonRNodes;
  private int numRNodes;
  private Node[] rnodes;
  private int[] tnodes;
  private int[] cnodes;
  private double targetx;
  private double targety;
  private double carx;
  private double cary;

  public Grid(int nn, int nnrn, int nrn){
    numNodes = nn;
    numNonRNodes = nnrn;
    numRNodes = nrn;
    rnodes = new Node[numRNodes];
    tnodes = new int[3];
    cnodes = new int[3];
    targetx = 0;
    targety = 0;
    carx = 0;
    cary = 0;
    initializeNodes();
  }

  /*
  * Will initialize the nodes based on NodeInput.txt which
  * provides the nodes' coordinates.
  */
  public void initializeNodes(){
    File inputfile = new File("NodeInput.txt");
    int n = 0;
    try {
      Scanner input = new Scanner(inputfile);
      String line;
      String[] splitline;
      Node rnode;
      while(input.hasNextLine()){
        rnode = new Node();
        line = input.nextLine();
        splitline = line.split("\\s+");
        rnode.x = Double.parseDouble(splitline[0]);
        rnode.y = Double.parseDouble(splitline[1]);
        rnodes[n++] = rnode;
      }
    }
    catch(FileNotFoundException e){
      e.printStackTrace();
    }



    System.out.println(n+" relay nodes configured"); // debug statement

    for(int i = 0; i < rnodes.length; i++){
      rnodes[i].td = 9999;
      rnodes[i].cd = 9999;
      System.out.println("Node "+i+" is at(x,y): ("+rnodes[i].x+", "+rnodes[i].y+")"); // debug statement
    }
  }

  /*
  * Updates a node with a specified distance, once nodeid == numNodes all nodes should have
  * been updated so attempt to locateTarget/Car
  */
  public void updateNodeDistance(int nodeid, double distance, int target){
    if(target == 0){
      rnodes[nodeid-(numNonRNodes+1)].td = distance;
      if(nodeid == numNodes){
        locateTarget();
      }
    }
    else if(target == 1){
      rnodes[nodeid-(numNonRNodes+1)].cd = distance;
      if(nodeid == numNodes){
        locateCar();
      }
    }
  }


  /*
  * Attempt to locateTarget using trilateration
  */
  public void locateTarget(){
    calculateSmallest(0);
    int n1 = tnodes[0];
    int n2 = tnodes[1];
    int n3 = tnodes[2];

    System.out.println("Calculating using nodes:");
    System.out.println(tnodes[0] + " " + tnodes[1] + " " + tnodes[2]);

    double d12 = 0;
    double d13 = 0;
    double j = 0;
    double k = 0;
    double angle13 = 0;
    double targetj = 0;
    double targetk = 0;

    // d12 = sqrt((x1-x2)^2 + (y1-y2)^2)
    d12 = Math.sqrt(Math.pow(Math.abs(rnodes[n1].x - rnodes[n2].x), 2) + Math.pow(Math.abs(rnodes[n1].y - rnodes[n2].y), 2));
    d13 = Math.sqrt(Math.pow(Math.abs(rnodes[n1].x - rnodes[n3].x), 2) + Math.pow(Math.abs(rnodes[n1].y - rnodes[n3].y), 2));
    angle13 = Math.atan(Math.abs(rnodes[n1].y - rnodes[n3].y)/Math.abs(rnodes[n1].x - rnodes[n3].x));
    j = d13*Math.cos(angle13);
    k = d13*Math.sin(angle13);
    // change to be Math.abs()
    targetj = Math.abs((Math.pow(rnodes[n1].td, 2) - Math.pow(rnodes[n2].td, 2) + Math.pow(d12, 2))/(2*d12));
    targetk = Math.abs((Math.pow(rnodes[n1].td,2) - Math.pow(rnodes[n3].td,2) + Math.pow(j,2) + Math.pow(k,2))/(2*k) - (j*targetj)/k);

//    System.out.println("d12 = "+d12);
//    System.out.println("d13 = "+d13);
//    System.out.println("angle13 = "+angle13);
//    System.out.println("(j, k) = ("+j+", "+k+")");

    //have relative position now convert (j, k) to (x, y)
    //convert j to x
    double angle12 = Math.atan(Math.abs(rnodes[n1].y - rnodes[n2].y)/Math.abs(rnodes[n1].x - rnodes[n2].x));
    if(n1 <= n2){
      targetx = rnodes[n1].x + (targetj * Math.cos(angle12));
    }
    else{
      targetx = rnodes[n1].x - (targetj * Math.cos(angle12));
    }
    //convert k to y
    double d1t = Math.sqrt(Math.pow(targetj, 2) + Math.pow(targetk, 2));
    double angle1t = Math.atan(targetk/targetj);
    if(n1 <= n3){
      targety = rnodes[n1].y + (d1t * Math.sin(angle12+angle1t));
    }
    else{
      targety = rnodes[n1].y - (d1t * Math.sin(angle12+angle1t));
    }


    System.out.println("Target is at (x,y) = ("+targetx+", "+targety+").");
    System.out.println();

    for(int i = 0; i < rnodes.length; i++){
      rnodes[i].td = 9999; // reset all lengths
    }
  }

  /*
  * Attempt to locateCar using trilateration
  */
  public void locateCar(){
    calculateSmallest(1);
  }

  /*
  * Will calculate the smallest 3 distances in relation to the target (mcase == 0)
  * or the car (mcase == 1)
  */
  public void calculateSmallest(int mcase){
    if(mcase == 0){ //tnodes (target nodes)
      tnodes[0] = 97; // 90's should get phased out as real data spills in, set to 90's for debugging
      tnodes[1] = 98;
      tnodes[2] = 99;
      double min1 = 9997;
      double min2 = 9998;
      double min3 = 9999;

      for(int i = 0; i < rnodes.length; i++){
        if(rnodes[i].td < min1){
          tnodes[2] = tnodes[1];
          tnodes[1] = tnodes[0];
          tnodes[0] = i;
          min3 = min2;
          min2 = min1;
          min1 = rnodes[i].td;
        }
        else if(rnodes[i].td < min2){
          tnodes[2] = tnodes[1];
          tnodes[1] = i;
          min3 = min2;
          min2 = rnodes[i].td;
        }
        else if(rnodes[i].td < min3){
          tnodes[2] = i;
          min3 = rnodes[i].td;
        }
      }
    }
    else if(mcase == 1){ //cnodes (car nodes)
      cnodes[0] = 97; // 90's should get phased out as real data spills in, set to 90's for debugging
      cnodes[1] = 98;
      cnodes[2] = 99;
      double min1 = 9997;
      double min2 = 9998;
      double min3 = 9999;

      for(int i = 0; i < rnodes.length; i++){
        if(rnodes[i].cd < min1){
          cnodes[2] = cnodes[1];
          cnodes[1] = cnodes[0];
          cnodes[0] = i;
          min3 = min2;
          min2 = min1;
          min1 = rnodes[i].cd;
        }
        else if(rnodes[i].cd < min2){
          cnodes[2] = cnodes[1];
          cnodes[1] = i;
          min3 = min2;
          min2 = rnodes[i].cd;
        }
        else if(rnodes[i].cd < min3){
          cnodes[2] = i;
          min3 = rnodes[i].cd;
        }
      }
    }
  }

}
