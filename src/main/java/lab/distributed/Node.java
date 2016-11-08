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
            sendBootstrapBroadcast();
            startTCPServerSocket();
    }

    /**
     *  broadcast eigen adres en naam op het netwerk
     */
    private void sendBootstrapBroadcast() {
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
     * @param hash de id van te verwijderen node
     */
    private void deleteNode(int hash)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if(!nameServerInterface.removeNode(hash))
                System.out.println("deze node bestaat niet");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
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
            return nameServerInterface.getOwner(fileName);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  deze node wordt verwijderd uit de nameserver en sluit af
     */
    public void exit()
    {
        updateNode(previousNode, nextNode, "next");     //naar de previous node het id van de next node sturen
        updateNode(nextNode, previousNode, "prev");     //naar de next node het id van de previous node sturen
        deleteNode(hashName(name));                     //node verwijderen uit de nameserver
        System.exit(0);
    }

    /**
     * Start de multicast listener op. Ontvang multicasts van andere nodes en worden hier behandeld
     */
    private void startMulticastListener() {
        new Thread(new Runnable() {
            public void run() {
                int hash = 0;
                try {
                    MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
                    multicastSocket.joinGroup(InetAddress.getByName(GROUP));
                    byte[] buf = new byte[256];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    while (true) {
                        multicastSocket.receive(datagramPacket);
                        byte[] byteAddress = Arrays.copyOfRange(buf, 0, 3);
                        String address = InetAddress.getByAddress(byteAddress).getHostAddress();
                        String name = new String(Arrays.copyOfRange(byteAddress, 4, 255)).trim();
                        hash = hashName(name);

                        /*
                        Indien nieuwe node tussen vorige node en deze node ligt, update vorige node en vertel tegen
                        nieuwe node zijn buren.
                         */
                        if (nextNode > hash && hash > myHash) { //tussen mezelf en de volgende hash
                            nextNode = hash;
                            Socket socket = new Socket(address, COMMUNICATIONS_PORT);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF("prev " + myHash);
                            dataOutputStream.writeUTF("next " + nextNode);

                            dataOutputStream.close();
                        }
                        /**
                         * Anders, als nieuwe node tussen mij en vorige node ligt, pas aan.
                         */
                        else if (myHash > hash && hash > previousNode)
                            previousNode = hash;

                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    failure(hash);
                }
            }
        }).start();
    }

    /**
     * Verstuur een multicast bericht naar alle nodes en nameserver met message als bericht
     * @param message het bericht dat verzonden moet worden
     */
    private void sendMulticast(byte[] message) {
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
     * de methode die moet aangeroepen worden wanneer de communicatie met een Node mislukt is
     * @param hash het id van de node waarmee de communicatie mislukt is
     */
    private void failure(int hash){
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            int nextNode = nameServerInterface.getNextNode(hash);
            int previousNode = nameServerInterface.getPreviousNode(hash);

            updateNode(previousNode,nextNode,"next");       //naar de previous node het id van de next node sturen
            updateNode(nextNode,previousNode,"prev");       //naar de next node het id van de previous node sturen
            deleteNode(hash);                               //node verwijderen

        } catch (RemoteException | MalformedURLException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met deze methode wordt de de volgende of vorige node van een bepaalde node aangepast
     * @param target de node waarin de parameters worden aangepast
     * @param aanpassing de nieuwe waarde voor de parameter
     * @param nextPrev  moet de volgende of de vorige node aangepast worden? kan waarde "next" of "prev" aannemen
     */
    private void updateNode(int target, int aanpassing, String nextPrev) {
        Socket socket;
        DataOutputStream dataOutputStream;
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            socket = new Socket(nameServerInterface.getAddress(target), COMMUNICATIONS_PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(nextPrev + " " + aanpassing); //als nextPrev een verkeerde waarde heeft wordt dit opgevangen in de listener
            dataOutputStream.close();
        } catch (NotBoundException | MalformedURLException | UnknownHostException | RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            failure(target);
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
    private void startTCPServerSocket(){
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

    /**
     * hash genereren van een bepaalde naam
     * @param name de naam waarvan de hash wordt gegenereerd
     * @return  de gegenereerde hash
     */
    public static final int hashName(String name) {
        return Math.abs(name.hashCode() % 32768);
    }


}
