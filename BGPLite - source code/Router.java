import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// initially running the program: ./router 4 ripd.conf
public class Router {
        private String name;
        private Boolean isAlreadySent;
        private Lock lock;

        // used for the purpose of routing
        private List<LinkInfo> routingTable;
        // used for the purpose of broadcasting the updates
        private Map<String, String> neighbors;
        private Map<String, Integer> neighborsWeight;

        public static void main(String[] args) {
                String name = args[0];
                String fileName = args[1];

                Router r1 = new Router(name, fileName);
        }

        public Router(String name, String fileName) {
                this.name = name;
                this.isAlreadySent = new Boolean(false);
                lock = new ReentrantLock();
                fillInitialRoutingTableAndNeighbors(fileName);

                // first fire up the ear of the router to listen and learn from the updates from the neighbors
                // It listens on the port 
                RouterEar ear = new RouterEar(routingTable, neighbors, neighborsWeight, isAlreadySent, lock, this.name);
                new Thread(ear).start();

                try {Thread.sleep(10000);} catch(Exception e) {e.printStackTrace();}

                lock.lock();
                if (isAlreadySent == false) { // we need to broadcast
                        broadcast(routingTable);
                }
                lock.unlock();
        }

        // this function will broadcast the routing table to all its neighbors
        private void broadcast(List<LinkInfo> routingTable) {
                for (Map.Entry<String, String> entry: neighbors.entrySet()) {
                        String ipAddress = entry.getKey();
                        Socket s = null;
                        boolean isConnected = false;
                        while (!isConnected) {
                                try {
                                        s = new Socket(ipAddress, 8000);
                                        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                                        out.writeObject(routingTable);
                                        out.flush();
                                        isConnected = true;
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
        }

        // this function will fill out the initial routing table and also the neighbors
        // the foramt of the file to be read is: dst-from-weight-router/host-IPADDRESS
        private void fillInitialRoutingTableAndNeighbors(String fileName) {
                routingTable = Collections.synchronizedList(new ArrayList<LinkInfo>());
                neighbors = Collections.synchronizedMap(new HashMap<String, String>());
                neighborsWeight = Collections.synchronizedMap(new HashMap<String, Integer>());

                BufferedReader br = null;
                try {
                        String line;
                        br = new BufferedReader(new FileReader(fileName));
                        while((line = br.readLine()) != null) {
                                String tokens[] = line.split("\\$");
                                String dst = tokens[0];
                                String from = tokens[1];
                                String weightString = tokens[2];
                                boolean isRouter = (tokens[3].equals("router") ? true : false);
                                String ipAddress = tokens[4];

                                if (weightString.equals("#")) {
                                        neighbors.put(ipAddress, from);
                                }
                                else {
                                        int weight = Integer.parseInt(tokens[2]);
                                        LinkInfo link = new LinkInfo(dst, from, weight, isRouter);
                                        routingTable.add(link);

                                        if (isRouter) {
                                                neighbors.put(ipAddress, from);
                                        }
                                        neighborsWeight.put(from, weight);
                                }
                        }
                }
                catch (IOException e) {
                        e.printStackTrace();
                }
                finally {
                        try {
                                if (br != null)
                                        br.close();
                        }
                        catch(IOException ex) {
                                ex.printStackTrace();
                        }
                }
        }
}