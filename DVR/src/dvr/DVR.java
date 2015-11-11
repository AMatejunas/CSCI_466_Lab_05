package dvr;

// Imports
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import java.util.Arrays;
import java.util.TimerTask;
import java.util.Timer;



public class DVR {
    
    private static byte[] sendData;         // data to be sent
       
    private static boolean[] sequenceAck;   // holds whether each packet has been acknowledged
    private static boolean[] sent;          // holds whether each packet has been sent
    
    //private static int dropped;             // the packet to be dropped
    //private static int maxSeq;              // holds the maximum sequence number
    private static int windowSize = 1;          // holds the size of the window
    private static int windowIndex = 0;         // holds the index of the first packet in the window
    private static int maxSeq = 1;              // holds the maximum sequence number
    
    private static InetAddress IPAdress;    // holds the IPAddress of the system
    private static Timer[] timers;          // holds the timers for each packet
    private static int[] ports;
    
    final static int X = 0;
    final static int Y = 1;
    final static int Z = 2;
    
    public static void main(String[] args) throws Exception {
           System.out.print("Enter the rounter's ID: ");
           BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
           
           // Read in router id
           char id = buff.readLine().charAt(0); // place holder; my id
           id = Character.toLowerCase(id);      // Forced formatting
           int me = -1; // index of this router
           
           if        (id == 'x') {
               me = X;
           } else if (id == 'y') {
               me = Y;
           } else if (id == 'z') {
               me = Z;
           } else { // invalid id, exit program
               System.out.println("Error: Invalid id");
               System.exit(0);
           }
          
           
           // Get port number and distance vector for this router and ports of
           // the other two routers
           FileReader file = new FileReader("configuration.txt");
           
           buff = new BufferedReader(file);
           
           // Get port numbers
           String line = buff.readLine();
           String[] fileData = line.split("\\s+");
           ports = new int[fileData.length];
           for (int i = 0; i < fileData.length; i++) {
               ports[i] = Integer.valueOf(fileData[i]);
           }
           
           // Get this router's distance vector
           // Search through lines until find this router's distance vector
           for (int i = 0; i <= me; i++) {
               line = buff.readLine();
           }
           fileData = line.split("\\s+");
           int[] distVec = new int[fileData.length]; // place holder; my distance vector
           for (int i = 0; i < fileData.length; i++) {
               distVec[i] = Integer.valueOf(fileData[i]);
           }
           
           // Print this router's info
           System.out.printf("Router %c is running on port %d\n", id, ports[me]);
           System.out.printf("Distance vector on router %c is:\n", id);
           printDistanceVector(distVec);
           
           // Initialize socket
           byte[] receiveData = new byte[12];
           DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
           DatagramSocket senderSocket = new DatagramSocket(ports[me]);
           
           // Send distance vector to other two routers
           sendDistanceVector(distVec, me, senderSocket);
           // Enter receive mode ad infinitum and update distance vector upon reception
           while(true) {
               receiveData = receivePacket.getData();
               // Set up a int buffer to convert bytes to ints
               IntBuffer intBuf = ByteBuffer.wrap(receiveData).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
               // Fill the new vector with the ints
               int[] tempVec = new int[intBuf.remaining()];
               // Check to see if an update occurred
               boolean update = false;
               for (int i = 0; i < tempVec.length && tempVec.length == distVec.length; i++) {
                   // See if a shorter path is present
                   // Oops, kind of did this before I realize what you implemented
                   // with update distance vector.
                   if (distVec[i] > tempVec[me] + tempVec[i]) {
                       distVec[i] = tempVec[me] + tempVec[i];
                       update = true;
                   }
               }
               if (update) {
                   // send data to other sockets and return to receive state
                   sendDistanceVector(distVec, me, senderSocket);
               }
           }
    }
    
    
    // print out inputted distance vector
    public static void printDistanceVector(int[] vector) {
        System.out.printf("<%d", vector[0]);
        for(int i = 1; i < vector.length; i++) {
            System.out.printf(", %d", vector[i]);
        }
        System.out.println(">");
    }
    
    // Sends a distance vector to the other two vectors
    private static void sendDistanceVector(int[] vector, int id, DatagramSocket sender) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    // Write the input vector to a data stream
    try {
        for(int i : vector) {
            dos.writeInt(i);
        }
    } catch (Exception e) {
        System.err.printf("%n%nA fatal error occurred!%n%n");
        e.printStackTrace();
        System.exit(1);
    }
    
    byte[] sendData = baos.toByteArray();
        for (int i = 0; i < 3; i++) {
            if (i != id) {
                try {
                    sendPacket(0, sendData, sender, false, ports[i]);
                } catch (Exception e) {
                    System.out.printf("%nSomething bad happened while trying to send the packet, carry on%n");
                }
            }
        }
    }
    
    // updates this server's distance vector based on received info and returns whether nor not a value was changed
    private static boolean updateDistanceVector(int[] myVector, int[] recVector, int recRouter) {
        int[] testVector = Arrays.copyOf(myVector, myVector.length);
        for (int i = 0; i < myVector.length; i++) {
            myVector[i] = min(myVector[i], myVector[recRouter] + recVector[i]);
        }
        return !Arrays.equals(myVector, testVector);
    }
    
    private static int min(int num1, int num2) {
        return Math.min(num1, num2);
    }
    
       // sends packet and sets up timer
    public static void sendPacket(int seq, byte[] sendData, DatagramSocket senderSocket, boolean drop, int portNumber)
            throws Exception {
        final int seqFinal = seq;
        final byte[] sendDataFinal = sendData;
        final DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAdress, portNumber);
        if (!drop) {
            senderSocket.send(sendPacket);
        }
        sent[seqFinal] = true;
        timers[seqFinal] = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Packet " + seqFinal + " times out, resend packet " + seqFinal);
                    sendPacket(seqFinal, sendDataFinal, senderSocket, false, portNumber);
                } catch (Exception e) {

                }
            }
        };
        timers[seqFinal].schedule(timerTask, 1000);
        printWindow();  
    }

    // sends all packets in window that have not yet been sent
    public static void sendWindow(DatagramSocket senderSocket, int portNumber)
            throws Exception {
        for (int i = windowIndex; i < windowIndex + windowSize; i++) {
            if (i >= maxSeq) {
                break;
            }
            if (!sequenceAck[i] && !sent[i]) {
                String sentence = String.valueOf(i);
                sendData = sentence.getBytes();
               
                sendPacket(i, sendData, senderSocket, false, portNumber);              
            }
        }
    }

    // advances windowIndex to next value
    private static void advanceWindow() {
        windowIndex++;
        while (windowIndex < maxSeq && sequenceAck[windowIndex]) {
            windowIndex++;
        }
    }

    // prints the window
    private static void printWindow() {
        System.out.print("window [");
        if (windowIndex < maxSeq) {
            System.out.print(windowIndex);
            if (sent[windowIndex] && !sequenceAck[windowIndex]) {
                System.out.print('*');
            }
        } else {
            System.out.print('-');
        }
        for (int i = windowIndex + 1; i < windowIndex + windowSize; i++) {
            if (i < maxSeq) {
                System.out.print(", " + i);
                if (sent[i] && !sequenceAck[i]) {
                    System.out.print('*');
                }
            } else {
                System.out.print(", -");
            }
        }
        System.out.println("]");
    }
}
