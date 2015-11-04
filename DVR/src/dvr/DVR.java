package dvr;

// Imports
import java.io.*;
import java.net.*;

import java.util.Arrays;

public class DVR {

    public static void main(String[] args) throws Exception {
           System.out.print("Enter the rounter's ID: ");
           
           // Read in router id
           char id = 'X'; // place holder; my id
           
           // Get port number and distance vector for this router and ports of
           // the other two routers
           int[] ports = {6666, 6667, 6668}; // place holder; ports in network
           int me = 0; // place holder; index of my port
           int[] distVec = {0, 2, 7}; // place holder; my distance vector
           
           System.out.printf("Router %c is running on port %d\n", id, ports[me]);
           System.out.printf("Distance vector on router %c is:\n", id);
           printDistanceVector(distVec);
           
           // Send distance vector to other two routers
           
           // Enter receive mode ad infinitum and update distance vector upon reception
    }
    
    
    // print out inputted distance vector
    public static void printDistanceVector(int[] vector) {
        System.out.printf("<%d", vector[0]);
        for(int i = 1; i < vector.length; i++) {
            System.out.printf(", %d", vector[i]);
        }
        System.out.println(">");
    }
    
    private static boolean updateDistanceVector(int[] myVector, int[] recVector, int recRouter) {
        int[] testVector = Arrays.copyOf(myVector, myVector.length);
        for (int i = 0; i < myVector.length; i++) {
            myVector[i] = min(myVector[i], myVector[recRouter] + recVector[i]);
        }
        return Arrays.equals(myVector, testVector);
    }
    
    private static int min(int num1, int num2) {
        return (num1 < num2) ? num1 : num2;
    }
}
