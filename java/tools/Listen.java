import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;
import java.util.*;

public class Listen {
  static int numnodes = 5;
  static int numrelaynodes = numnodes - 2;
  static Node[] rnodes = new Node[numrelaynodes]; // relay nodes
  static int[] tnodes = new int[3]; // 3 relay nodes used to calculate target location
  static int[] cnodes = new int[3]; // 3 relay nodes used to calculate car location

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
          byte[] nodeid_b = {0x00,0x00,packet[8],packet[9]};
          byte[] rssi_b = {0x00,0x00,packet[10],packet[11]};

          if(packet[10] == (byte)0xFF){
            rssi_b[0] = (byte)0xFF;
            rssi_b[1] = (byte)0xFF;
          }
          int nodeid = ByteBuffer.wrap(nodeid_b).getInt();
          if(nodeid != 2){
            int rssi = ByteBuffer.wrap(rssi_b).getInt();
            int rssi_dbm = rssi - 45;
            int a = -45; // dBm at 1 m/Transmission power
            double n = 2.7; // propagation constant [2, 2.7]
            double d = Math.pow(10, ((a - rssi_dbm)/(10*n)));
          System.out.println("Node " + nodeid + " is " + d + " m away from the target. (" + rssi_dbm + "dBm)");
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
    rnodes[0].x = 0.0; // node's x position
    rnodes[0].y = 0.0; // node's y position
    rnodes[1].x = 0.6233; // node's x position
    rnodes[1].y = 0.0; // node's y position
    rnodes[2].x = 0.34925; // node's x position
    rnodes[2].y = 0.9144; // node's y position
  }

  public static void updateNode(int nodeid, double distance){
    rnodes[nodeid-numrelaynodes].td = distance;
    if(nodeid == numnodes){
      locateTarget();
    }
  }

  public static void locateTarget(){
//    for(int i = 0; i < tdistances.length; i++){
//      System.out.println("Node " + (int)(i+numrelaynodes) + " is " + tdistances[i] + " m away from the target.");
//    }
    /*
    Node 1 at (0,0), Node 2 at (d, 0), Node 3 at (i, j) (i along x axis, j along y axis from Node 1)
    x = (r1^2 - r2^2 + d^2)/(2*d)
    y = (r1^2 - r3^2 + j^2 + k^2)/(2*k) - (j*x)/k
    use distance equation to figure out d, j, k
    */
    calculateSmallest(0);
    int n1 = tnodes[0];
    int n2 = tnodes[1];
    int n3 = tnodes[2];

    double targetx = 0;
    double d12 = 0;
    double d13 = 0;
    double j = 0;
    double k = 0;
    double angle13 = 0;
    double targety = 0;

    // d12 = sqrt((x1-x2)^2 + (y1-y2)^2)
    d12 = Math.sqrt(Math.pow(Math.abs(rnodes[n1].x - rnodes[n2].x), 2) + Math.pow(Math.abs(rnodes[n1].y - rnodes[n2].y), 2));
    targetx = Math.abs((Math.pow(rnodes[n1].td, 2) - Math.pow(rnodes[n2].td, 2) + Math.pow(d12, 2))/(2*d12));
    d13 = Math.sqrt(Math.pow(Math.abs(rnodes[n1].x - rnodes[n3].x), 2) + Math.pow(Math.abs(rnodes[n1].y - rnodes[n3].y), 2));
    angle13 = Math.atan(Math.abs(rnodes[n1].x - rnodes[n3].x)/Math.abs(rnodes[n1].y - rnodes[n3].y));
    j = d13*Math.cos(angle13);
    k = d13*Math.sin(angle13);
    targety = Math.abs((Math.pow(rnodes[n1].td,2) - Math.pow(rnodes[n3].td,2) + Math.pow(j,2) + Math.pow(k,2))/(2*k) - (j*targetx)/k);

    System.out.println("d12 = "+d12);
    System.out.println("d13 = "+d13);
    System.out.println("angle13 = "+angle13);
    System.out.println("(j, k) = ("+j+", "+k+")");
    System.out.println("Target is at (x,y) = ("+targetx+", "+targety+").");

    for(int i = 0; i < rnodes.length; i++){
      System.out.println("Node " + (int)(i+numrelaynodes) + " is " + rnodes[i].td + " m away from the target.");
    }
    System.out.println();
  }

  public static void calculateSmallest(int mcase){
    if(mcase == 0){ //tnodes (target nodes)
      tnodes[0] = 7;
      tnodes[1] = 7;
      tnodes[2] = 7;
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
      System.out.println("not yet implemented");
      // should calculate things for car distances here or something
    }
  }

}
