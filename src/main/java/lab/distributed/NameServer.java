package lab.distributed;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.TreeMap;

/**
 * Nameserver in system y
 * Created by Ivan on 12/10/2016.
 */
@XmlRootElement(name = "nameserver")
public class NameServer implements NameServerInterface {

    /**
     * Multicast Config
     */
    public static final String GROUP = "225.1.2.3";
    public static final int MULTICAST_PORT = 12345;
    public static final int COMMUNICATIONS_PORT = 4000;
    @XmlElement(name = "nodemap")
    TreeMap<Integer, String> nodeMap = new TreeMap<>();

    public NameServer() {
        startMulticastListener();
    }

    /**
     * laad de nameserver van de disk
     * @return
     */
    public static NameServer fromDisk() {
        try {
            JAXBContext context = JAXBContext.newInstance(NameServer.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (NameServer) unmarshaller.unmarshal(new File("nameserver.xml"));
        } catch (JAXBException e) {
            e.printStackTrace();
            System.out.println("File probably not found...");
            return null;
        }
    }

    /**
     * hash genereren van een bepaalde naam
     *
     * @param name de naam waarvan de hash wordt gegenereerd
     * @return de gegenereerde hash
     */
    public static final int hashName(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    @Override
    public boolean addNode(String nodeName, String inetAddress) {
        if (!nodeMap.containsKey(hashName(nodeName))) {
            nodeMap.put(hashName(nodeName), inetAddress);
            saveToDisk();
            return true;
        } else
            return false;
    }

    @Override
    public boolean removeNode(int nodeName) {
        boolean temp = nodeMap.remove(nodeName) != null;
        if(temp) {
        System.out.printf("Node with hash %d left. New sitation: %s\n", nodeName, nodeMap.toString());
        saveToDisk();
        }
        return temp;
    }

    @Override
    public int getPreviousNode(int hash) {
        Integer previous = nodeMap.lowerKey(hash);
        return previous != null ? previous.intValue() : nodeMap.lastKey();
    }

    @Override
    public int getNextNode(int hash) {
        Integer next = nodeMap.higherKey(hash);
        return next != null ? next.intValue() : nodeMap.firstKey();
    }

    @Override
    public String getOwner(String filename) {
        int fileHash = hashName(filename);
        int closest = -1;
        for (Integer integer : nodeMap.keySet()) {
            if (integer > closest && integer < fileHash) {
                closest = integer;
            } else
                return nodeMap.get(closest == -1 ? nodeMap.lastKey() : closest);
        }
        if (closest != -1)
            return nodeMap.get(closest);
        else return null;
    }

    /**
     * slaag de nameserver op op de schijf
     */
    public void saveToDisk() {
        try {
            JAXBContext context = JAXBContext.newInstance(NameServer.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, new File("nameserver.xml"));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start de multicast listener op. Ontvang multicasts van andere nodes en worden hier behandeld
     */
    public void startMulticastListener() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
                    multicastSocket.joinGroup(InetAddress.getByName(GROUP));
                    Socket socket;
                    while (true) {
                        byte[] buf = new byte[256];
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(datagramPacket);
                        byte[] byteAddress = Arrays.copyOfRange(buf, 0, 4);
                        String address = InetAddress.getByAddress(byteAddress).getHostAddress();
                        String name = new String(Arrays.copyOfRange(buf, 4, 255)).trim();
                        System.out.println("Received multicast with IP " + address + " and name " + name);
                        NodeInterface node = getNode(address);
                        if (addNode(name, address)) {
                            node.setSize(Inet4Address.getLocalHost().getHostAddress(), nodeMap.size());
                            System.out.printf("Node %s from %s requested to join and was accepted.\nNew sitation: %s\n", name, address, nodeMap.toString());
                        }
                        else {
                            node.setSize(Inet4Address.getLocalHost().getHostAddress(), -1);
                            System.out.printf("Node %s from %s requested to join but was rejected due to duplicate\n", name, address);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public String getAddress(int hash) {
        return nodeMap.get(hash);
    }

    /**
     * geeft de interface van een node terug zodat deze via rmi kan aangeroepen worden
     * @param IP het ip adres van de gevraagde node
     * @return de interface van de gevraagde node
     */
    public NodeInterface getNode(String IP) {
        String name = String.format("//%s/NodeInterface", IP);
        try {
            return (NodeInterface) Naming.lookup(name);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }


}
