import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;
import java.util.*;

public class Trilaterate {
  static int numNodes = 6; // total number of nodes
  static int numNonRNodes = 1+1; // number of sending motes + base station (not relay nodes)
  static int numRNodes = numNodes - numNonRNodes;
  static Grid grid = new Grid(numNodes, numNonRNodes, numRNodes);

  public static void main(String args[]) throws IOException {
    String source = null;

    PacketSource reader;
    if (args.length == 2 && args[0].equals("-comm")) {
      source = args[1];
    }
    else if (args.length > 0) {
      System.err.println("usage: java Trilaterate [-comm PACKETSOURCE]");
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
      while(true){
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
            grid.updateNodeDistance(nodeid, d, 0); // update target distance
          }
          System.out.flush();
        }
      }
    }
    catch (IOException e) {
      System.err.println("Error on " + reader.getName() + ": " + e);
    }
  }
}
