package lab.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Scanner;


public class Node {

    /**
     * Multicast Config
     */
    public static final String GROUP = "225.1.2.3";
    public static final int MULTICAST_PORT = 12345;
    public static final int COMMUNICATIONS_PORT = 4000;
    public static final int PING_PORT = 9000;
    private String name;
    private int myHash;
    private String location;
    private String nameServerName = "//192.168.1.1/NameServerInterface";
    private int previousNode;
    private int nextNode;

    /**
     * De constructor gaat een nieuwe node aanmaken in de nameserver met de gekozen naam en het ip adres van de machine waarop hij gestart wordt.
     *
     * @param name de naam van de node
     */
    public Node(String name) {
        this.name = name;
        this.myHash = hashName(name);
        try {
            location = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        startMulticastListener();
        try {
            Thread.sleep(500); // Start TCP socket a second after multicast listener to prevent deadlock.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startTCPServerSocket();

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

    /**
     * broadcast eigen adres en naam op het netwerk
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
     *
     * @param hash de id van te verwijderen node
     */
    public void deleteNode(int hash) {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if (!nameServerInterface.removeNode(hash))
                System.out.println("Deze node bestaat niet");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }



    }

    /**
     * Deze methode zal de locatie van een bestand opzoeken in de nameserver
     *
     * @param fileName de naam van het te zoeken bestand
     * @return het ip adres van de locatie van het bestand
     */
    public String getFileLocation(String fileName) {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            return nameServerInterface.getOwner(fileName);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * deze node wordt verwijderd uit de nameserver en sluit af
     */
    public void exit() {
        System.out.println("Leaving the network and updating my neighbours...");
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
                    while (true) {
                        byte[] buf = new byte[256];
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(datagramPacket);
                        byte[] byteAddress = Arrays.copyOfRange(buf, 0, 4);
                        String address = InetAddress.getByAddress(byteAddress).getHostAddress();
                        String name = new String(Arrays.copyOfRange(buf, 4, 255)).trim();
                        hash = hashName(name);

                        /**
                         * Ga eerst na of we de enigste node waren in het netwerk. Zo ja, zet vorige en volgende naar
                         * de nieuwe node, en zet die van de nieuwe node naar ons.
                         * https://gyazo.com/f0a9b650813f46d1b98ac63bb6b396fb
                         */
                        if(previousNode == myHash && nextNode == myHash) {
                            previousNode = hash;
                            nextNode = hash;
                            Socket socket = new Socket(address, COMMUNICATIONS_PORT);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF("prev " + myHash);
                            dataOutputStream.writeUTF("next " + myHash);
                            dataOutputStream.close();
                            System.out.println("A second node has joined. I've set my previous and next node to him and updated him.");
                        }
                        /**
                         * Hierna gaan we na of de node tussen ons en één van onze buren ligt
                         */
                        else if((myHash < hash && hash < nextNode)
                                || (nextNode < myHash && (hash > myHash || hash < nextNode))) {
                            /**
                             * SITUATIE 1: (eerste deel van if-case)
                             * De node ligt tussen mij en mijn volgende buur. De nieuwe node is mijn volgende en ik ben
                             * de vorige van de nieuwe node. Ik zeg dit tegen de nieuwe node en pas mijn volgende aan.
                             */
                            /**
                             * SITUATIE 2: (tweede monstreuze deel van if-case)
                             * Ik zit aan het einde van de kring want mijn volgende node is lager dan mij.
                             * De nieuwe node ligt boven mij, of ligt onder mijn volgende (laagste) node. Ik licht
                             * de nieuwe node in over zijn buren en pas mijn volgende aan.
                             */
                            Socket socket = new Socket(address, COMMUNICATIONS_PORT);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF("prev " + myHash);
                            dataOutputStream.writeUTF("next " + nextNode);
                            dataOutputStream.close();
                            System.out.printf("A node (%d) joined between me (%d) and my next neighbour (%d). Updating accordingly...\nWelcome %s!\n",hash, myHash, nextNode, name);
                            nextNode = hash;
                        }
                        else if ((previousNode < hash && hash < myHash)
                                || (previousNode > myHash && (hash < myHash || hash > nextNode))) {
                            /**
                             * De node ligt tussen mijn vorige buur en mij. Mijn vorige buur zal de nieuwe node
                             * over zijn nieuwe buren informeren. Ik pas enkel mijn vorige node aan.
                             */
                            System.out.printf("A node (%d) joined between my previous neighbour (%d) and me. Updating accordingly...\n" +
                                    "Welcome %s!\n",hash, previousNode, name);
                            previousNode = hash;
                        } else if (hash == myHash){
                            System.out.printf("I joined the network.\n");
                        }else{
                            System.out.printf("A node (%d) joined but isn't between my previous or next neighbour.\n" +
                                    "Welcome %s!\n",hash, name);
                        }
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
     *
     * @param message het bericht dat verzonden moet worden
     */
    private void sendMulticast(byte[] message) {
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket(MULTICAST_PORT, InetAddress.getLocalHost());
            datagramSocket.send(new DatagramPacket(message, message.length, InetAddress.getByName(GROUP), MULTICAST_PORT));
            System.out.println("Multicast sent from " + name);
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
     *
     * @param hash het id van de node waarmee de communicatie mislukt is
     */
    private void failure(int hash) {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            int nextNode = nameServerInterface.getNextNode(hash);
            int previousNode = nameServerInterface.getPreviousNode(hash);

            updateNode(previousNode, nextNode, "next");       //naar de previous node het id van de next node sturen
            updateNode(nextNode, previousNode, "prev");       //naar de next node het id van de previous node sturen
            deleteNode(hash);                               //node verwijderen

        } catch (RemoteException | MalformedURLException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met deze methode wordt de de volgende of vorige node van een bepaalde node aangepast
     *
     * @param target     de node waarin de parameters worden aangepast
     * @param aanpassing de nieuwe waarde voor de parameter
     * @param nextPrev   moet de volgende of de vorige node aangepast worden? kan waarde "next" of "prev" aannemen
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
     * <p>
     * codewoorden
     * size param1 = size
     * prev param1 = previous id param1
     * next param1 = next id param1
     */
    private void startTCPServerSocket() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Integer size = null;
                    Integer newNextNode = null;
                    Integer newPreviousNode = null;
                    ServerSocket serverSocket = new ServerSocket(COMMUNICATIONS_PORT);
                    sendBootstrapBroadcast();
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                        while(true) {
                            String buf;
                            try {
                            buf = dataInputStream.readUTF();}
                            catch (IOException e) {
                                break; //When the socket closes an IO exception get's thrown. Break the loop and wait
                                        //for the next command...
                            }
                            String[] splitted = buf.split("\\s");
                            switch (splitted[0]) {
                                case "size":
                                    size = Integer.parseInt(splitted[1]);
                                    System.out.println("Found Nameserver on IP " + clientSocket.getInetAddress().getHostAddress());
                                    if (size == 1) {
                                        System.out.println("I'm the first node. I'm also the previous and next node. ");
                                        previousNode = myHash;
                                        nextNode = myHash;
                                    } else {
                                        System.out.printf("I'm not the first node (size is %d). Waiting for my next and previous node...\n", size);
                                    }
                                    break;
                                case "prev":
                                    newPreviousNode = Integer.parseInt(splitted[1]);
                                    previousNode = newPreviousNode;
                                    System.out.println("My previous node was updated by " + clientSocket.getInetAddress().getHostAddress() + " to " + previousNode);
                                    break;
                                case "next":
                                    newNextNode = Integer.parseInt(splitted[1]);
                                    nextNode = newNextNode;
                                    System.out.println("My next node was updated by " + clientSocket.getInetAddress().getHostAddress() + " to " + nextNode);
                                    break;
                                case "duplicate":
                                    System.out.println("Deze naam bestaat al in het domein.");
                                    System.out.println("Geef een nieuwe naam");
                                    Scanner scanner = new Scanner(System.in);
                                    name = scanner.nextLine();
                                    sendBootstrapBroadcast();
                                    break;
                            }
                        }

                        /*if (size != null && newNextNode != null && newPreviousNode != null) {
                            if (size < 1) {
                                previousNode = myHash;
                                nextNode = myHash;
                            } else {
                                nextNode = newNextNode;
                                previousNode = newPreviousNode;
                            }
                        }*/
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * dit is slechts een testmethode om de failure methode op te roepen.
     */
    public void sendPing(){

        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            Socket socket = new Socket(nameServerInterface.getAddress(nextNode), PING_PORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            while(true){
                dataOutputStream.writeUTF("ping");
                String pong = dataInputStream.readUTF();
                if(pong.equals("pong")){
                    dataOutputStream.writeUTF("ping");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            failure(nextNode);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }


    }

    /**
     * Testmethode om bij sendping methode te gaan, persoon die zal ontvangen moet eerst receiveping starten, persoon die zal senden moet dan sendping starten, kabel uittrekken van persoon die receiveping draait.
     */
    public void receivePing(){
        try {
            ServerSocket serverSocket = new ServerSocket(PING_PORT);
            Socket clientSocket = serverSocket.accept();
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            while(true) {
                String ping = dataInputStream.readUTF();
                if (ping.equals("ping")) {
                    dataOutputStream.writeUTF("pong");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
