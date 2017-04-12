import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;

public class Listen {
  static int numnodes = 5;
  static int numrelaynodes = numnodes - 2;
  static double[] distances = new double[numrelaynodes];
  static double[][] relaynodes = new double[numrelaynodes][2];


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
            int a = -45; // dBm at 1 m/Transmission power
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
    relaynodes[0][0] = 0; // node's x position
    relaynodes[0][1] = 0; // node's y position
    relaynodes[1][0] = 45; // node's x position
    relaynodes[1][1] = 90; // node's y position
    relaynodes[2][0] = 90; // node's x position
    relaynodes[2][1] = 0; // node's y position
  }

  public static void updateNode(int nodeid, double distance){
    distances[nodeid-numrelaynodes] = distance;
    if(nodeid == numnodes){
      locate();
    }
  }

  public static void locate(){
//    for(int i = 0; i < distances.length; i++){
//      System.out.println("Node " + (int)(i+numrelaynodes) + " is " + distances[i] + " m away from the target.");
//    }
    /*
    Node 1 at (0,0), Node 2 at (d, 0), Node 3 at (i, j) (i along x axis, j along y axis from Node 1)
    x = (r1^2 - r2^2 + d^2)/(2*d)
    y = (r1^2 - r3^2 + i^2 + j^2)/(2*j) - (i*x)/j
    */

  }

}
