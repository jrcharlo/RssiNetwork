// $Id: Listen.java,v 1.5 2010-06-29 22:07:41 scipio Exp $

/*									tab:4
 * Copyright (c) 2000-2003 The Regents of the University  of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the University of California nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright (c) 2002-2003 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA,
 * 94704.  Attention:  Intel License Inquiry.
 */


import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import java.nio.ByteBuffer;
import java.lang.Math.*;

public class Listen {
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
//          System.out.println("Node " + nodeid + " received signal strength of " + rssi_dbm + " dBm.");
          int a = -45; // dBm at 1 m/Transmission power
          double n = 2.7; // propagation constant [2, 2.7]
          double d = Math.pow(10, ((a - rssi_dbm)/(10*n)));
//          System.out.println("Node " + nodeid + " is " + d*100 + " cm away from the target. (" + rssi_dbm + "dBm)");
          System.out.println(rssi_dbm);
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
