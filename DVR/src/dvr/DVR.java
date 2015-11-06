package dvr;

// Imports
import java.io.*;
import java.net.*;

import java.util.Arrays;

public class DVR {

    public static void main(String[] args) throws Exception {
           System.out.print("Enter the rounter's ID: ");
           BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
           
           // Read in router id
           char id = buff.readLine().charAt(0); // place holder; my id
           
           int me = -1; // index of this router
           if (id == 'X' || id == 'x') {
               me = 0;
           } else if (id == 'Y' || id == 'y') {
               me = 1;
           } else if (id == 'Z' || id == 'z') {
               me = 2;
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
           int[] ports = new int[fileData.length];
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
    
    // updates this server's distance vector based on received info and returns whether nor not a value was changed
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
