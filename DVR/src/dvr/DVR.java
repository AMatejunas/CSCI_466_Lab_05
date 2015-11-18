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
    private static boolean[] sent = new boolean[2];          // holds whether each packet has been sent

    //private static int dropped;             // the packet to be dropped
    //private static int maxSeq;              // holds the maximum sequence number
    private static int windowSize = 1;          // holds the size of the window
    private static int windowIndex = 0;         // holds the index of the first packet in the window
    private static int maxSeq = 1;              // holds the maximum sequence number
    private static int me;

    private static InetAddress IPAdress;    // holds the IPAddress of the system
    private static Timer[] timers = new Timer[2];          // holds the timers for each packet
    private static int[] ports;
    private static int[] neighVec;
    private static int[][] distVecs;
    
    final static int X = 0;
    final static int Y = 1;
    final static int Z = 2;

    final static char[] ids = {'X', 'Y', 'Z'};

    public static void main(String[] args) throws Exception {
        System.out.print("Enter the rounter's ID: ");
        BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));

        // Read in router id
        char id = buff.readLine().charAt(0); // place holder; my id
        id = Character.toUpperCase(id);      // Forced formatting
        me = -1; // index of this router

        if (id == 'X') {
            me = X;
        } else if (id == 'Y') {
            me = Y;
        } else if (id == 'Z') {
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
        int[] myDistVec = new int[fileData.length]; // place holder; my distance vector
        for (int i = 0; i < fileData.length; i++) {
            myDistVec[i] = Integer.valueOf(fileData[i]);
        }
        neighVec = Arrays.copyOf(myDistVec, myDistVec.length);
        distVecs = new int[fileData.length][fileData.length];
        int[] infVector = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        for (int i = 0; i < fileData.length; i++) {
            if (i == me) {
                distVecs[i] = myDistVec;
            } else {
                distVecs[i] = Arrays.copyOf(infVector, infVector.length);
            }
        }

        // Print this router's info
        System.out.printf("Router %c is running on port %d\n", id, ports[me]);
        System.out.printf("Distance vector on router %c is:\n", id);
        printDistanceVector(myDistVec);

        // Initialize socket
        byte[] receiveData = new byte[16];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        DatagramSocket senderSocket = new DatagramSocket(ports[me]);
        IPAdress = InetAddress.getByName("localhost");

        // Send distance vector to other two routers
        sendDistanceVector(myDistVec, me, senderSocket);
        // Enter receive mode ad infinitum and update distance vector upon reception
        while (true) {
            senderSocket.receive(receivePacket);
            receiveData = receivePacket.getData();

            // Set up a buffer to convert bytes to ints
            ByteBuffer wrapped = ByteBuffer.wrap(receiveData);

            // Fill the new vector with the ints
            int recID = wrapped.getInt();
            int[] tempVec = new int[3];
            for (int i = 0; i < tempVec.length; i++) {
                tempVec[i] = wrapped.getInt();
            }
            int[] testVec = {0, 0, 0};
            if (!Arrays.equals(tempVec, testVec)) {
                char recIDChar = '-';
                if (recID == 0) {
                    recIDChar = 'X';
                } else if (recID == 1) {
                    recIDChar = 'Y';
                } else if (recID == 2) {
                    recIDChar = 'Z';
                }

                System.out.printf("Receives distance vector from router %c: ", recIDChar);
                printDistanceVector(tempVec);
                // Check to see if an update occurred
                distVecs[recID] = tempVec;
                boolean update = updateDistanceVector(myDistVec, recID);
                char myId = ids[me];
                if (update) {
                    System.out.printf("Distance vector on router %c is updated to:\n", myId);
                    printDistanceVector(myDistVec);
                    sendDistanceVector(myDistVec, me, senderSocket);
                } else {
                    System.out.printf("Distance vector on router %c is not updated\n", myId);
                }
            }

            receiveData = new byte[16];
        }
    }

    // print out inputted distance vector
    public static void printDistanceVector(int[] vector) {
        System.out.printf("<%d", vector[0]);
        for (int i = 1; i < vector.length; i++) {
            System.out.printf(", %d", vector[i]);
        }
        System.out.println(">");
    }

    // Sends a distance vector to the other two vectors
    private static void sendDistanceVector(int[] vector, int id, DatagramSocket sender) {
        ByteBuffer b = ByteBuffer.allocate(16);
        // Write the input vector to a data stream
        try {
            b.putInt(id);
            for (int i : vector) {
                b.putInt(i);
            }
        } catch (Exception e) {
            System.err.printf("%n%nA fatal error occurred!%n%n");
            e.printStackTrace();
            System.exit(1);
        }

        sendData = b.array();
        //System.out.println(sendData[2]);
        for (int i = 0; i < 3; i++) {
            if (i != id) {
                try {
                    sendPacket(0, sendData, sender, false, ports[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.printf("%nSomething bad happened while trying to send the packet, carry on%n");
                }
            }
        }
    }

    // updates this server's distance vector based on received info and returns whether nor not a value was changed
    private static boolean updateDistanceVector(int[] myVector, int recRouter) {
        int[] testVector = Arrays.copyOf(myVector, myVector.length);
        for (int i = 0; i < myVector.length; i++) {
            int value = neighVec[i];
            for (int j = 0; j < myVector.length; j++) {
                int newValue = neighVec[j] + distVecs[j][i];
                if (newValue < 0) {
                    newValue = Integer.MAX_VALUE;
                }
                if (newValue < value) {
                    value = newValue;
                }
            }
            myVector[i] = value;
        }
        return !Arrays.equals(myVector, testVector);
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

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //System.out.println("Packet " + seqFinal + " times out, resend packet " + seqFinal);
                    sendPacket(seqFinal, sendDataFinal, senderSocket, false, portNumber);
                } catch (Exception e) {

                }
            }
        };
        if (portNumber == ports[(me + 1) % 3]) {
            sent[0] = true;
            timers[0] = new Timer();
            timers[0].schedule(timerTask, 1000);
        } else if (portNumber == ports[(me + 2) % 3]) {
            sent[1] = true;
            timers[1] = new Timer();
            timers[1].schedule(timerTask, 1000);
        }
    }
}
