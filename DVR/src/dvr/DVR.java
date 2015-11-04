package dvr;

// Imports
import java.io.*;
import java.net.*;

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
    
    public static void printDistanceVector(int[] vector) {
        System.out.print("<");
        //System.out.print(String.join(",", vector));
    }
}
