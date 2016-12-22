import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class RTUpdater implements Runnable {
        private Boolean isAlreadySent;
        private Lock lock;
        private List<LinkInfo> routingTable;
        private Map<String, String> neighbors;
        private Map<String, Integer> neighborsWeight;
        private Socket socket;
        private String routerIP;
        private Boolean isConverged;
        private Lock lock2;
        private long startTime;

        public RTUpdater(Socket socket, String routerIP, List<LinkInfo> routingTable, Map<String, String> neighbors,
                        Map<String, Integer> neighborsWeight, Boolean isAlreadySent, Lock lock, Boolean isConverged, Lock lock2, long startTime) {
                this.socket = socket;
                this.routerIP = routerIP;
                this.routingTable = routingTable;
                this.neighbors = neighbors;
                this.neighborsWeight = neighborsWeight;
                this.isAlreadySent = isAlreadySent;
                this.lock = lock;
                this.isConverged = isConverged;
                this.lock2 = lock2;
                this.startTime = startTime;
        }

        public void run() {
                //System.out.printf("routerIP is %s\n", routerIP);
                boolean isConverged = true;
                ObjectInputStream in;
                List<LinkInfo> receivedRoutingTable = null;
                try {
                        in = new ObjectInputStream(this.socket.getInputStream());
                        receivedRoutingTable = (List<LinkInfo>) in.readObject();
                } catch (IOException e) {
                        e.printStackTrace();
                } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                }

                if (neighborsWeight.get(neighbors.get(routerIP)) == null) {
                        System.out.println("----------------------------");
                        System.out.println("Time, taken so far is (in milliseconds): " + (System.currentTimeMillis() - this.startTime));
                        for (LinkInfo link: routingTable) {
                                System.out.printf("dst: %s, from: %s, weight: %d\n", link.getDst(), link.getFrom(), link.getWeight());
                        }
                        return;
                }

                // trying to update the local routing table, based on the received routing table
                for (LinkInfo receivedLink: receivedRoutingTable) {
                        boolean found = false;
                        ListIterator<LinkInfo> iter = routingTable.listIterator();
                        while (iter.hasNext()) {
                                LinkInfo localLink = iter.next();
                                if (localLink.getDst().equals(receivedLink.getDst())) {
                                        found = true;
                                        if (receivedLink.getWeight() + neighborsWeight.get(neighbors.get(routerIP)) < localLink.getWeight()) {
                                                isConverged = false;
                                                localLink.setFrom(neighbors.get(routerIP));
                                                localLink.setWeight(receivedLink.getWeight() + neighborsWeight.get(neighbors.get(routerIP)));
                                        }
                                }
                        }

                        if (!found) {
                                LinkInfo newLink = new LinkInfo(receivedLink.getDst(), neighbors.get(routerIP),
                                        receivedLink.getWeight() + neighborsWeight.get(neighbors.get(routerIP)), receivedLink.isRouter());
                                routingTable.add(newLink);
                                isConverged = false;
                        }
                }

                System.out.println("----------------------------");
                System.out.println("Time, taken so far is (in milliseconds): " + (System.currentTimeMillis() - this.startTime));
                for (LinkInfo link: routingTable) {
                        System.out.printf("dst: %s, from: %s, weight: %d\n", link.getDst(), link.getFrom(), link.getWeight());
                }

                if (isConverged) {
                        lock2.lock();
                        this.isConverged = true;
                        lock2.unlock();

                        try {
                                socket.close();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
                else {

                        lock.lock();
                        this.isAlreadySent = true;
                        lock.unlock();

                        try {
                                socket.close();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }

                        broadcast(routingTable);
                }
        }

        // this function will broadcast the initial routing table to all its neighbors
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

}