import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;

public class Listen {
  static int numnodes = 5;
  static int numrelaynodes = numnodes - 2;
  static double[] tdistances = new double[numrelaynodes]; //target distances
  static int[] targetd = new int[3]; // 3 closest nodes to target
  static double[][] rnodes = new double[numrelaynodes][2];


  public static void main(String args[]) throws IOException {
    String source = null;

    PacketSource reader;
    if (args.length == 2 && args[0].equals("-comm")) {
      source = args[1];
    }
    else if (args.length > 0) {
      System.err.println("usage: java net.tinyos.tools.Listen [-comm PACKETSOURCE]");
      System.err.println("       (default packet source from MOTECOM environment variable)");
      System.exit(2);
    }
    if (source == null) {
      reader = BuildSource.makePacketSource();
    }
    else {
      reader = BuildSource.makePacketSource(source);
    }
    if (reader == null) {
      System.err.println("Invalid packet source (check your MOTECOM environment variable)");
      System.exit(2);
    }

    try {
      reader.open(PrintStreamMessenger.err);
      initializeNodes();
      for (;;) {
        byte[] packet = reader.readPacket();
        if(packet.length == 12){
//  	    Dump.printPacket(System.out, packet);
//  	    System.out.println();
          byte[] nodeid_b = {0x00,0x00,packet[8],packet[9]};
          byte[] rssi_b = {0x00,0x00,packet[10],packet[11]};

          if(packet[10] == (byte)0xFF){
            rssi_b[0] = (byte)0xFF;
            rssi_b[1] = (byte)0xFF;
          }
//        Dump.printPacket(System.out, nodeid_b);
//        Dump.printPacket(System.out, rssi_b);
//        System.out.println();
          int nodeid = ByteBuffer.wrap(nodeid_b).getInt();
          if(nodeid != 2){
            int rssi = ByteBuffer.wrap(rssi_b).getInt();
            int rssi_dbm = rssi - 45;
            int a = -50; // dBm at 1 m/Transmission power
            double n = 2.7; // propagation constant [2, 2.7]
            double d = Math.pow(10, ((a - rssi_dbm)/(10*n)));
//          System.out.println("Node " + nodeid + " received signal strength of " + rssi_dbm + " dBm.");
//          System.out.println("Node " + nodeid + " is " + d*100 + " cm away from the target. (" + rssi_dbm + "dBm)");
//          System.out.println(rssi_dbm);
            updateNode(nodeid, d);
          }
          System.out.flush();
        }
      }
    }
    catch (IOException e) {
      System.err.println("Error on " + reader.getName() + ": " + e);
    }
  }

  public static void initializeNodes(){
    rnodes[0][0] = 0.0; // node's x position
    rnodes[0][1] = 0.0; // node's y position
    rnodes[1][0] = 0.32; // node's x position
    rnodes[1][1] = 0.0; // node's y position
    rnodes[2][0] = 0.22; // node's x position
    rnodes[2][1] = 0.46; // node's y position
  }

  public static void updateNode(int nodeid, double distance){
    tdistances[nodeid-numrelaynodes] = distance;
    if(nodeid == numnodes){
      locateTarget();
    }
  }

  public static void locateTarget(){
    for(int i = 0; i < tdistances.length; i++){
      System.out.println("Node " + (int)(i+numrelaynodes) + " is " + tdistances[i] + " m away from the target.");
    }
    /*
    Node 1 at (0,0), Node 2 at (d, 0), Node 3 at (i, j) (i along x axis, j along y axis from Node 1)
    x = (r1^2 - r2^2 + d^2)/(2*d)
    y = (r1^2 - r3^2 + j^2 + k^2)/(2*k) - (j*x)/k
    use distance equation to figure out d, j, k
    */
    calculateSmallest(0);
    int n1 = targetd[0];
    int n2 = targetd[1];
    int n3 = targetd[2];
    double d12 = 0;
    // d12 = sqrt((x1-x2)^2 + (y1-y2)^2)
    d12 = Math.sqrt(Math.pow(Math.abs(rnodes[n1][0] - rnodes[n2][0]), 2) + Math.pow(Math.abs(rnodes[n1][1] - rnodes[n2][1]), 2));
    double targetx = 0;
    targetx = (Math.pow(tdistances[n1], 2) - Math.pow(tdistances[n2], 2) + Math.pow(d12, 2))/(2*d12);
    double d13 = 0;
    d13 = Math.sqrt(Math.pow(Math.abs(rnodes[n1][0] - rnodes[n3][0]), 2) + Math.pow(Math.abs(rnodes[n1][1] - rnodes[n3][1]), 2));
    double angle13 = 0;
    angle13 = Math.atan(Math.abs(rnodes[n1][0] - rnodes[n3][0])/Math.abs(rnodes[n1][1] - rnodes[n3][1]));
    double j = 0;
    double k = 0;
    j = d13*Math.cos(angle13);
    k = d13*Math.sin(angle13);
    double targety = 0;
    targety = (Math.pow(tdistances[n1],2) - Math.pow(tdistances[n3],2) + Math.pow(j,2) + Math.pow(k,2))/(2*k) - (j*Math.abs(targetx))/k;

//    System.out.println("d12 = "+d12);
//    System.out.println("d13 = "+d13);
//    System.out.println("angle13 = "+angle13);
//    System.out.println("(j, k) = ("+j+", "+k+")");
    System.out.println("Target is at (x,y) = ("+Math.abs(targetx)+", "+Math.abs(targety)+").");
  }

  public static void calculateSmallest(int mcase){
    if(mcase == 0){
      for(int i = 0; i < 3; i++){ // stupid scheme that works because there are 3 nodes
        // this should ideal place the index of the smallest distance in tdistances at 0
        // second smallest at 1, largest at 2
        targetd[i] = i;
      }
    }
    else if(mcase == 1){
      System.out.println("not yet implemented");
      // should calculate things for car distances here or something
    }
  }

}
