package dvr;

// Imports
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import java.util.Arrays;
import java.util.TimerTask;
import java.util.Timer;

public class DVR {
    final static int ROUTERS = 3; // the number of routers in the network
    
    // the indices of each id in the ports, neighVec and distVecs arrays
    final static int X = 0;
    final static int Y = 1;
    final static int Z = 2;
    
    
    private static int me;                  // my index for the ports, neighVec and distVecs arrays

    private static int[] ports;             // the ports to be used
    private static int[] neighVec;          // the direct cost to each neighbor
    private static int[][] distVecs;        // the shortest distance cost vector to each node from each node

    final static char[] ids = {'X', 'Y', 'Z'};
    
    private static byte[] sendData;         // data to be sent
    private static InetAddress IPAdress;    // holds the IPAddress of the system
    private static Timer[] timers = new Timer[2];          // holds the timers for most recently sent packet to each neighbor
    private static int seq = -1;            // the value of the most recently sent sequence number, initialized to none sent

    public static void main(String[] args) throws Exception {
        System.out.print("Enter the router's ID: ");
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
        ports = new int[ROUTERS];
        for (int i = 0; i < ports.length; i++) {
            ports[i] = Integer.valueOf(fileData[i]);
        }

        // Get this router's distance vector
        // Search through lines until find this router's distance vector
        for (int i = 0; i <= me; i++) {
            line = buff.readLine();
        }
        fileData = line.split("\\s+");
        distVecs = new int[ROUTERS][ROUTERS];
        
        // make my distance vector
        for (int i = 0; i < distVecs[me].length; i++) {
            distVecs[me][i] = Integer.valueOf(fileData[i]);
        }
        neighVec = Arrays.copyOf(distVecs[me], distVecs[me].length);

        int[] infVector = new int[ROUTERS];
        for (int i = 0; i < infVector.length; i++) {
            infVector[i] = Integer.MAX_VALUE;
        }
        for (int i = 0; i < distVecs.length; i++) {
            if (i != me) {
                distVecs[i] = Arrays.copyOf(infVector, infVector.length);
            }
        }

        // Print this router's info
        System.out.printf("Router %c is running on port %d\n", id, ports[me]);
        System.out.printf("Distance vector on router %c is:\n", id);
        printDistanceVector(distVecs[me]);

        // Initialize socket
        byte[] receiveData = new byte[24];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        DatagramSocket socket = new DatagramSocket(ports[me]);
        IPAdress = InetAddress.getByName("localhost");

        // Send distance vector to other two routers
        sendDistanceVector(socket);
        // Enter receive mode ad infinitum and update distance vector upon reception
        while (true) {
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();

            // Set up a buffer to convert bytes to ints
            ByteBuffer wrapped = ByteBuffer.wrap(receiveData);

            // Fill the new vector with the ints
            int recID = wrapped.getInt();

            int ack = wrapped.getInt(); // get whether this is an ack or not
            if (ack == 1) {
                if (wrapped.getInt() == seq) {
                    // Cancel appropriate timer
                    for (int i = 1; i < ROUTERS; i++) {
                        if (recID == (me + i) % ROUTERS) {
                            timers[i - 1].cancel();
                        }
                    }
                }
            } else {
                int recSeq = wrapped.getInt(); // grab sequence number
                int[] tempVec = new int[ROUTERS];
                for (int i = 0; i < tempVec.length; i++) {
                    tempVec[i] = wrapped.getInt();
                }
                sendAck(recSeq, ports[recID], socket); // send acknowledgement
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
                    char myId = ids[me];
                    boolean update = updateDistanceVector();
                    if (update) {
                        System.out.printf("Distance vector on router %c is updated to:\n", myId);
                        printDistanceVector(distVecs[me]);
                        sendDistanceVector(socket);
                    } else {
                        System.out.printf("Distance vector on router %c is not updated\n", myId);
                    }
                }
            }

            receiveData = new byte[24];
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

    // Sends an acknowledgement packet
    private static void sendAck(int seq, int dest, DatagramSocket sender)
            throws Exception {

        ByteBuffer b = ByteBuffer.allocate(24);

        try {
            b.putInt(me);
            b.putInt(1); // this is an ack
            b.putInt(seq); // put seq number to acknowledge in packet
        } catch (Exception e) {
            System.err.printf("%n%nA fatal error occurred!%n%n");
            e.printStackTrace();
            System.exit(1);
        }

        sendData = b.array();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAdress, dest);
        sender.send(sendPacket);
    }

    // Sends a distance vector to the other two vectors
    private static void sendDistanceVector(DatagramSocket sender) {
        ByteBuffer b = ByteBuffer.allocate(24);
        // Write the input vector to a data stream
        try {
            b.putInt(me);
            b.putInt(0); // this is not an ack
            b.putInt(++seq); // increment sequence number and put in buffer
            for (int i : distVecs[me]) {
                b.putInt(i);
            }
        } catch (Exception e) {
            System.err.printf("%n%nA fatal error occurred!%n%n");
            e.printStackTrace();
            System.exit(1);
        }

        sendData = b.array();
        for (int i = 0; i < ROUTERS; i++) {
            if (i != me) {
                try {
                    sendPacket(0, sendData, sender, ports[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.printf("%nSomething bad happened while trying to send the packet, carry on%n");
                }
            }
        }
    }

    // updates this server's distance vector based on received info and returns whether nor not a value was changed
    private static boolean updateDistanceVector() {
        int[] testVector = Arrays.copyOf(distVecs[me], distVecs[me].length);
        for (int i = 0; i < distVecs[me].length; i++) {
            int value = neighVec[i];
            for (int j = 0; j < distVecs[me].length; j++) {
                int newValue = neighVec[j] + distVecs[j][i];
                if (newValue < 0) {
                    newValue = Integer.MAX_VALUE;
                }
                if (newValue < value) {
                    value = newValue;
                }
            }
            distVecs[me][i] = value;
        }
        return !Arrays.equals(distVecs[me], testVector);
    }

    // sends packet and sets up timer
    public static void sendPacket(int seq, byte[] sendData, DatagramSocket senderSocket, int dest)
            throws Exception {
        final int seqFinal = seq;
        final byte[] sendDataFinal = sendData;
        final DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAdress, dest);
        senderSocket.send(sendPacket);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    //System.out.println("Packet " + seqFinal + " times out, resend packet " + seqFinal);
                    sendPacket(seqFinal, sendDataFinal, senderSocket, dest);
                } catch (Exception e) {

                }
            }
        };
        for (int i = 0; i < ROUTERS - 1; i++) {
            if (dest == ports[(me + i + 1) % ROUTERS]) {
                if (timers[i] != null) {
                    timers[i].cancel();
                }
                timers[i] = new Timer();
                timers[i].schedule(timerTask, 1000); 
            }
        }
    }
}
