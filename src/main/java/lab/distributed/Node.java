package lab.distributed;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.InputMismatchException;


public class Node {

    private String name;
    private int myHash;
    private String location;
    private String nameServerName = "//192.168.1.1/NameServerInterface";

    private int previousNode;
    private int nextNode;






    /**
     * Multicast Config
     */
    public static final String GROUP = "225.1.2.3";
    public static final int MULTICAST_PORT = 12345;

    public static final int COMMUNICATIONS_PORT =  4000;

    /**
     * De constructor gaat een nieuwe node aanmaken in de nameserver met de gekozen naam en het ip adres van de machine waarop hij gestart wordt.
     * @param name de naam van de node
     */
    public Node(String name){
        this.name=name;
        this.myHash=hashName(name);
        try {
            location=InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {//dit moet er nog uit want dit is dubbel werk
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if(!nameServerInterface.addNode(name,location))
            {
                System.out.println("deze naam bestaat al");
                System.exit(1);
            }
            sendBootstrapBroadcast();
            startTCPServerSocket();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void sendBootstrapBroadcast() {
        try {
            byte[] addressData = Inet4Address.getByName(location).getAddress();
            byte[] nameData = name.getBytes();
            byte[] message = new byte[addressData.length + nameData.length];
            System.arraycopy(addressData, 0, message, 0, addressData.length);
            System.arraycopy(nameData, 0, message, addressData.length, nameData.length);
            sendMulticast(message);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deze node kan eender welke node verwijderen uit de nameServer
     * @param name de naam van te verwijderen node
     */
    public void deleteNode(String name)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if(!nameServerInterface.removeNode(hashName(name)))
                System.out.println("deze node bestaat niet");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Deze methode zal de locatie van een bestand opzoeken in de nameserver
     * @param fileName de naam van het te zoeken bestand
     * @return het ip adres van de locatie van het bestand
     */
    public String getFileLocation(String fileName)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            String fileLocation = nameServerInterface.getOwner(fileName);
            return fileLocation;
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  deze node wordt verwijderd uit de nameserver en sluit af
     */
    public void exit()
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            deleteNode(name);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }finally {
            System.exit(0);
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
                    byte[] buf = new byte[256];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    while(true) {
                        multicastSocket.receive(datagramPacket);
                        byte[] byteAddress = Arrays.copyOfRange(buf, 0, 3);
                        String address = InetAddress.getByAddress(byteAddress).getHostAddress();
                        String name = new String(Arrays.copyOfRange(byteAddress, 4, 255)).trim();
                        int hash = hashName(name);

                        /*
                        Indien nieuwe node tussen vorige node en deze node ligt, update vorige node en vertel tegen
                        nieuwe node zijn buren.
                         */
                        if(previousNode < hash && hash < myHash) {
                            previousNode = hash;
                            Socket socket = new Socket(address, COMMUNICATIONS_PORT);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF("prev "+myHash);
                            dataOutputStream.writeUTF("next "+nextNode);

                            dataOutputStream.close();
                        }
                        /**
                         * Anders, als nieuwe node tussen mij en volgende node ligt, pas aan.
                         */
                        else if(myHash < hash && hash < nextNode)
                            nextNode = hash;

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Verstuur een multicast bericht naar alle nodes en nameserver met message als bericht
     * @param message
     */
    public void sendMulticast(byte[] message) {
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket(12345);
            datagramSocket.send(new DatagramPacket(message, message.length, InetAddress.getByName(GROUP), MULTICAST_PORT));
            datagramSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Start de TCPServerSocket, er wordt continu geluisterd voor wanneer
     * een node offline gaat.
     *
     * codewoorden
     * size param1 = size
     * prev param1 = previous id param1
     * next param1 = next id param1
     */
    public void startTCPServerSocket(){
        try {
            Integer size = null;
            Integer nextNode = null;
            Integer previousNode = null;
            ServerSocket serverSocket = new ServerSocket(COMMUNICATIONS_PORT);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                String buf = new String();
                try {
                    while (true) {
                        buf += dataInputStream.readUTF() + " ";
                    }
                } catch (IOException e) {
                    //wanneer exception gevangen wordt, wil dit zeggen dat client klaar is
                    //socket gaat gewoon voort luisteren naar andere inkomende verbindingen.
                }
                String[] splitted = buf.split("\\s");

                for (int i = 0; i < splitted.length / 2; i++) {
                    switch (splitted[i]) {
                        case "size":
                            size = Integer.parseInt(splitted[i + 1]);
                            break;
                        case "prev":
                            previousNode = Integer.parseInt(splitted[i + 1]);
                            break;
                        case "next":
                            nextNode = Integer.parseInt(splitted[i + 1]);
                            break;
                    }
                }
                if (size != null && nextNode != null && previousNode != null) {
                    if (size < 1) {
                        this.previousNode = myHash;
                        this.nextNode = myHash;
                    } else {
                        this.nextNode = nextNode;
                        this.previousNode = previousNode;
                    }
                }
            }
    } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final int hashName(String name) {
        return Math.abs(name.hashCode() % 32768);
    }


}
