import java.io.*;
import java.lang.Math.*;
import java.util.*;
import java.net.*;

public class Grid{
  private int numNodes;
  private int numNonRNodes;
  private int numRNodes;
  private Node[] rnodes;
  private int[] tnodes;
  private int[] cnodes;
  private Boolean tReady;
  private double targetx;
  private double targety;
  private Boolean cReady;
  private double carx;
  private double cary;
  private String carIP;
  private Socket carSocket;
  private char[][] grid;
  private int maxY;
  private int maxX;
  private int res;
  private int maxR;
  private int maxC;

  public Grid(int nn, int nnrn, int nrn){
    numNodes = nn;
    numNonRNodes = nnrn;
    numRNodes = nrn;
    rnodes = new Node[numRNodes];
    tnodes = new int[3];
    cnodes = new int[3];
    tReady = false;
    targetx = 10.0;
    targety = 0.0;
    cReady = false;
    carx = 0.0;
    cary = 0.0;
    carIP = "192.168.4.1";
    maxX = 200;
    maxY = 200;
    res = 5;
    maxR = maxY/res; // 5cm (rows represent y-axis)
    maxC = maxX/res; // 5cm (columns represent x-axis)
    initializeGrid();
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
      placeNode((int)rnodes[i].x, (int)rnodes[i].y, i+numNonRNodes+1); // remove numNonRNodes if want relative id displayed
    }
    printGrid();
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
    System.out.println(n1 + " " + n2 + " " + n3);

    System.out.println(n1 + ": " + rnodes[n1].td);
    System.out.println(n2 + ": " + rnodes[n2].td);
    System.out.println(n3 + ": " + rnodes[n3].td);

    updateTargetLoc(' ');

    double a, b, c, d, e, f;
    a = (-2*rnodes[n1].x) + (2*rnodes[n2].x);
    b = (-2*rnodes[n1].y) + (2*rnodes[n2].y);
    c = Math.pow(rnodes[n1].td, 2) - Math.pow(rnodes[n2].td, 2) - Math.pow(rnodes[n1].x, 2) + Math.pow(rnodes[n2].x, 2)
        - Math.pow(rnodes[n1].y, 2) + Math.pow(rnodes[n2].y, 2);
    d = (-2*rnodes[n2].x) + (2*rnodes[n3].x);
    e = (-2*rnodes[n2].y) + (2*rnodes[n3].y);
    f = Math.pow(rnodes[n2].td, 2) - Math.pow(rnodes[n3].td, 2) - Math.pow(rnodes[n3].x, 2) + Math.pow(rnodes[n3].x, 2)
        - Math.pow(rnodes[n3].y, 2) + Math.pow(rnodes[n3].y, 2);
    a = Math.abs(a);
    b = Math.abs(b);
    c = Math.abs(c);
    e = Math.abs(e);
    d = Math.abs(d);
    f = Math.abs(f);
    targetx = ((f*b)-(e*c))/((b*d)-(e*a));
    targety = ((a*e)-(c*d))/((a*e)-(d*b));
//    targetx = ((c*d)-(f*a))/((b*d)-(e*a));
//    targety = ((a*e)-(d*b))/((c*e)-(f*b));

    targetx = Math.abs(targetx);
    targety = Math.abs(targety);

//    System.out.println("Target is at (x,y) = ("+targetx+", "+targety+").");
    if(targetx < 0){
      targetx = 0;
    }
    else if(targetx > maxX){
      targetx = maxX;
    }
    if(targety < 0){
      targety = 0;
    }
    else if(targety > maxY){
      targety = maxY;
    }
//    System.out.println("Target is at (x,y) = ("+targetx+", "+targety+").");
//    System.out.println();

    tReady = true;
    updateTargetLoc('T');
    printGrid();
    if(tReady && cReady){
      sendtoCar();
    }
    for(int i = 0; i < rnodes.length; i++){
      rnodes[i].td = 9999; // reset all distances
    }

  }

  /*
  * Attempt to locateCar using trilateration
  */
  public void locateCar(){
    calculateSmallest(1);
    int n1 = cnodes[0];
    int n2 = cnodes[1];
    int n3 = cnodes[2];

    System.out.println("Calculating using nodes:");
    System.out.println(cnodes[0] + " " + cnodes[1] + " " + cnodes[2]);

    updateCarLoc(' ');

    double a, b, c, d, e, f;
    a = (-2*rnodes[n1].x) + (2*rnodes[n2].x);
    b = (-2*rnodes[n1].y) + (2*rnodes[n2].y);
    c = Math.pow(rnodes[n1].cd, 2) - Math.pow(rnodes[n2].cd, 2) - Math.pow(rnodes[n1].x, 2) + Math.pow(rnodes[n2].x, 2)
        - Math.pow(rnodes[n1].y, 2) + Math.pow(rnodes[n2].y, 2);
    d = (-2*rnodes[n2].x) + (2*rnodes[n3].x);
    e = (-2*rnodes[n2].y) + (2*rnodes[n3].y);
    f = Math.pow(rnodes[n2].cd, 2) - Math.pow(rnodes[n3].cd, 2) - Math.pow(rnodes[n3].x, 2) + Math.pow(rnodes[n3].x, 2)
        - Math.pow(rnodes[n3].y, 2) + Math.pow(rnodes[n3].y, 2);
    a = Math.abs(a);
    b = Math.abs(b);
    c = Math.abs(c);
    d = Math.abs(d);
    e = Math.abs(e);
    f = Math.abs(f);
    carx = ((f*b)-(e*c))/((b*d)-(e*a));
    cary = ((a*e)-(c*d))/((a*e)-(d*b));

//    System.out.println("Car is at (x,y) = ("+carx+", "+cary+").");
//    System.out.println();
    if(carx < 0){
      carx = 0;
    }
    else if(carx > maxR){
      carx = maxR;
    }
    if(cary < 0){
      cary = 0;
    }
    else if(cary > maxY){
      cary = maxY;
    }

    cReady = true;
    updateCarLoc('C');
    printGrid();
    if(tReady && cReady){
      sendtoCar();
    }
    for(int i = 0; i < rnodes.length; i++){
      rnodes[i].cd = 9999; // reset all distances
    }
  }

  /*
  * Will calculate the smallest 3 distances in relation to the target (mcase == 0)
  * or the car (mcase == 1)
  */
  public void calculateSmallest(int mcase){
    double min1 = 10000;
    double min2 = 10000;
    double min3 = 10000;
    if(mcase == 0){ //tnodes (target nodes)
      tnodes[0] = 97; // 90's should get phased out as real data spills in, set to 90's for debugging
      tnodes[1] = 98;
      tnodes[2] = 99;

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

  /*
  * Sends data to the car via TCP socket, car is running a server on port 8989.
  * Method should only be called when data is ready.
  */
  public void sendtoCar(){
    try{
      carSocket = new Socket();
      carSocket.connect(new InetSocketAddress(carIP, 8989), 5000);
      DataOutputStream dos = new DataOutputStream(carSocket.getOutputStream());

      String coords = carx+" "+cary+" "+targetx+" "+targety+" "; //null terminating " " is important! CarServer relies on it!
      dos.writeBytes(coords);

      carSocket.close();
    }
    catch (Exception e){
      e.printStackTrace();
    }

    for(int i = 0; i < rnodes.length; i++){
      rnodes[i].td = 9999; // reset all distances
      rnodes[i].cd = 9999; // reset all distances
    }
    tReady = false;
    cReady = false;
  }

  public void initializeGrid(){
    maxR++; maxC++;
    grid = new char[maxC][2*(maxR)];
    for(int i = 0; i <= 2*maxR-1; i+=2){
      for(int j = 0; j <= maxC-1; j++){
        grid[j][i] = ' ';
        grid[j][i+1] = ' ';
      }
    }
    printGrid();
  }

  public void placeNode(int x, int y, int id){
    grid[x/res][(2*y)/res] = 'N';
    grid[x/res][(2*y)/res+1] = (char)(id+48);
  }

  public void updateTargetLoc(char c){
    grid[(int)(targetx)/res][(int)(2*targety)/res] = c;
//    grid[(int)(targetx)/res][(int)(2*targety)/res+1] = c;
  }

  public void updateCarLoc(char c){
    grid[(int)(carx)/res][(int)(2*cary)/res] = c;
//    grid[(int)(carx)/res][(int)(2*cary)/res+1] = c;
  }

  public void printGrid(){
  String xrow = "#X" + new String(new char[(2*maxC)]).replace('\0', 'X') + "#";
  String brow = "###" + new String(new char[(2*maxC)]).replace('\0', '#');
  System.out.print("\033[H\033[2J");
  System.out.flush();
  System.out.println(brow);
  for(int i = 2*maxR-2; i >= 0; i-=2){
    for(int j = 0; j <= maxC-1; j++){
        if(j == 0){
//          System.out.print("XX");
          System.out.print("#Y");
          System.out.print(grid[j][i]+""+grid[j][i+1]);
        }
        else if(j == maxC-1){
          System.out.print(grid[j][i]+""+grid[j][i+1]);
          System.out.print("#");
//          System.out.print("XX");
        }
        else{
          System.out.print(grid[j][i]+""+grid[j][i+1]);
        }
      }
      System.out.println();
    }
    System.out.println(xrow);
    System.out.println(brow);
    System.out.println("Grid resolution is 5 cm. Legend: 'Y' is 5 cm, 'XX' is 5 cm.\n");
    System.out.println("Target (TT) location: ("+targetx+", "+targety+").");
    System.out.println("Car (CC) location: ("+carx+", "+cary+").");
  }

}
