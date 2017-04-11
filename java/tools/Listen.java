import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;

public class Listen {
  int numnodes = 5;
  int numrelaynodes = numnodes - 2;
  double[] distances = new double[numrelaynodes];
  int[][] relaynodes = new int[numrelaynodes][2];


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
          }
          System.out.flush();
        }
      }
    }
    catch (IOException e) {
      System.err.println("Error on " + reader.getName() + ": " + e);
    }
  }

  public void updateNode(int nodeid, double distance){
    distances[nodeid-numrelaynodes] = distance;
    if(nodeid == numnodes){
      locate();
    }
  }

  public void locate(){
    for(int i = 0; i < distances.length; i++){
      System.out.println("Node " + i+numrelaynodes + " is " + distances[i] + " cm away from target
      //hello
    }
  }

}
