import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;
import java.util.*;

public class Trilaterate {
  static int numNodes = 7; // total number of nodes
  static int numNonRNodes = 3; // number of sending motes + base station (not relay nodes)
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
        if(packet.length == 14){
          byte[] nodeid_b = {0x00,0x00,packet[8],packet[9]};
          byte[] rssi_b = {0x00,0x00,packet[10],packet[11]};
          byte[] onode_b = {0x00,0x00,packet[12],packet[13]};

          if(packet[10] == (byte)0xFF){
            rssi_b[0] = (byte)0xFF;
            rssi_b[1] = (byte)0xFF;
          }
          int nodeid = ByteBuffer.wrap(nodeid_b).getInt();
          if(nodeid > numNonRNodes){ // do not consider target/car packets
            int onode = ByteBuffer.wrap(onode_b).getInt(); // this is the original node (2 = target)
            int rssi = ByteBuffer.wrap(rssi_b).getInt();
            int rssi_dbm = rssi - 45;
            int a = -51; // dBm at 1 m / Transmission power
            double n = 2.7; // propagation constant [2, 2.7]
            double d = 100*Math.pow(10, ((a - rssi_dbm)/(10*n)));
            System.out.println("Node "+ nodeid +" is "+ d +" cm away from node "+ onode +". (" + rssi_dbm + "dBm)");
            if(onode == 2){
              grid.updateNodeDistance(nodeid, d, 0); // update target distance
            }
            else{
              grid.updateNodeDistance(nodeid, d, 1); // update car distance
            }
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
