import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RouterEar implements Runnable {
        private String name;
        private Boolean isAlreadySent;
        private Lock lock;
        private List<LinkInfo> routingTable;
        private Map<String, String> neighbors;
        private Map<String, Integer> neighborsWeight;
        private Boolean isConverged;
        private Lock lock2;

        public RouterEar(List<LinkInfo> routingTable, Map<String, String> neighbors,
                        Map<String, Integer> neighborsWeight, Boolean isAlreadySent, Lock lock, String name) {
                this.name = name;
                this.routingTable = routingTable;
                this.neighbors = neighbors;
                this.neighborsWeight = neighborsWeight;
                this.isAlreadySent = isAlreadySent;
                this.lock = lock;
                this.isConverged = new Boolean(false);
                this.lock2 = new ReentrantLock();
        }

        public void run() {
                long startTime = System.currentTimeMillis();
                ServerSocket serverSocket = null;
                try {
                         serverSocket = new ServerSocket(8000);
                         while (true) {
                                Socket socket = serverSocket.accept();
                                InetAddress inetAddres = socket.getInetAddress();
                                String routerIP = inetAddres.getHostAddress();

                                RTUpdater rtUpdater = new RTUpdater(socket, routerIP, routingTable, neighbors, neighborsWeight,
                                                isAlreadySent, lock, isConverged, lock2, startTime);
                                new Thread(rtUpdater).start();

                                lock2.lock();
                                if (isConverged) {
                                        System.out.printf("Converge happen in router %s. Here is the routing table:\n", this.name);
                                        for (LinkInfo link: routingTable) {
                                                System.out.printf("dst: %s, from: %s, weight: %d\n", link.getDst(), link.getFrom(), link.getWeight());
                                        }
                                        break;
                                }
                                lock2.unlock();
                        }
                }
                catch (IOException ex) {
                        ex.printStackTrace();
                }
                finally {
                        if (serverSocket != null)
                            try {
                                    serverSocket.close();
                            } catch (IOException e) {
                                    e.printStackTrace();
                            }
                }

        }
}