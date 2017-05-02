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
    targetx = 15.17;
    targety = 5.05;
    cReady = false;
    carx = 2.00;
    cary = 3.00;
    carIP = "192.168.4.1";
    maxR = 50; //cm (rows represent y-axis)
    maxC = 100; //cm (columns represent x-axis)
    initializeGrid();
    initializeNodes();

    System.exit(1); //testing
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
      placeNode((int)rnodes[i].x, (int)rnodes[i].y, i+numNonRNodes); // remove numNonRNodes if want relative id displayed
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
    System.out.println(tnodes[0] + " " + tnodes[1] + " " + tnodes[2]);

    updateTargetLoc(' ');

    double a, b, c, d, e, f;
    a = Math.abs((-2*rnodes[n1].x) + (2*rnodes[n2].x));
    b = Math.abs((-2*rnodes[n1].y) + (2*rnodes[n2].y));
    c = Math.abs(Math.pow(rnodes[n1].td, 2) - Math.pow(rnodes[n2].td, 2) - Math.pow(rnodes[n1].x, 2) + Math.pow(rnodes[n2].x, 2)
        - Math.pow(rnodes[n1].y, 2) + Math.pow(rnodes[n2].y, 2));
    d = Math.abs((-2*rnodes[n2].x) + (2*rnodes[n3].x));
    e = Math.abs((-2*rnodes[n2].y) + (2*rnodes[n3].y));
    f = Math.abs(Math.pow(rnodes[n2].td, 2) - Math.pow(rnodes[n3].td, 2) - Math.pow(rnodes[n3].x, 2) + Math.pow(rnodes[n3].x, 2)
        - Math.pow(rnodes[n3].y, 2) + Math.pow(rnodes[n3].y, 2));
    targetx = ((c*d)+(f*a))/((b*d)+(e*a));
    targety = ((a*e)+(b*d))/((c*e)+(f*b));

    System.out.println("Target is at (x,y) = ("+targetx+", "+targety+").");
    System.out.println();
    if(targetx < 0){
      targetx = 0;
    }
    else if(targetx > maxC){
      targetx = maxC;
    }
    if(targety < 0){
      targety = 0;
    }
    else if(cary > maxR){
      cary = maxR;
    }

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
    carx = ((c*d)+(f*a))/((b*d)+(e*a));
    cary = ((a*e)+(b*d))/((c*e)+(f*b));

    System.out.println("Car is at (x,y) = ("+carx+", "+cary+").");
    System.out.println();
    if(carx < 0){
      carx = 0;
    }
    else if(carx > maxC){
      carx = maxC;
    }
    if(cary < 0){
      cary = 0;
    }
    else if(cary > maxR){
      cary = maxR;
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
    updateTargetLoc('T');
    updateCarLoc('C');
    printGrid();
  }

  public void placeNode(int x, int y, int id){
    grid[x][2*y] = 'N';
    grid[x][2*y+1] = (char)(id+48);
  }

  public void updateTargetLoc(char c){
    grid[(int)(targetx)][(int)(2*targety)] = c;
    grid[(int)(targetx)][(int)(2*targety+1)] = c;
  }

  public void updateCarLoc(char c){
    grid[(int)(carx)][(int)(2*cary)] = c;
    grid[(int)(carx)][(int)(2*cary+1)] = c;
  }

  public void printGrid(){
  String xrow = "0" + new String(new char[(2*maxC)]).replace('\0', 'X');
  System.out.print("\033[H\033[2J");
  System.out.flush();
//  System.out.println(xrow);
  for(int i = 2*maxR-2; i >= 0; i-=2){
    for(int j = 0; j <= maxC-1; j++){
        if(j == 0){
//          System.out.print("XX");
          System.out.print("Y");
          System.out.print(grid[j][i]+""+grid[j][i+1]);
        }
        else if(j == maxC-1){
          System.out.print(grid[j][i]+""+grid[j][i+1]);
//          System.out.print("XX");
        }
        else{
          System.out.print(grid[j][i]+""+grid[j][i+1]);
        }
      }
      System.out.println();
    }
    System.out.println(xrow);
    System.out.println("Legend: 'Y' is 10 cm, 'X' is 5 cm.\n");
    System.out.println("Target (TT) location: ("+targetx+", "+targety+").");
    System.out.println("Car (CC) location: ("+carx+", "+cary+").");
  }

}
