package lab.distributed;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Een node in system y
 */
public class Node implements NodeInterface {

    private static final String GROUP = "225.1.2.3"; //multicast groep
    private static final int MULTICAST_PORT = 12345;
    private static final int FILESERVER_PORT = 4001;
    private static final int COMMUNICATIONS_PORT = 4000;
    private static final int PING_PORT = 9000;
    private String name;                            //naam van de node
    private int myHash;                             //hash van de node
    private String location;                        //ip adres van de node   
    private int previousNode = -1;                  //hash van de vorige node
    private int nextNode = -1;                      //hash van de huidige node
    private FileServer fileServer;                  //wordt gebruikt om bestanden te versturen over tcp
    private HashMap<String, FileEntry> localFiles, replicatedFiles; //key: naam, value: FileEntry
    private NameServerInterface nameServer;         //interface om de server via RMI te bereiken
    private WatchDir watchDir;
    private static final Path LOCAL_DIRECTORY = Paths.get("local");
    private static final Path REPLICATED_DIRECTORY = Paths.get("replicated");
    private HashMap<String,Boolean> fileList;
    private ArrayList<String> lockedFiles = new ArrayList<>();

    /**
     * De constructor gaat een nieuwe node aanmaken in de nameserver met de gekozen naam en het ip adres van de machine waarop hij gestart wordt.
     *
     * @param name de naam van de node
     */
    public Node(String name) {
        this.name = name;
        this.myHash = hashName(name);
        localFiles = new HashMap<>();
        replicatedFiles = new HashMap<>();
        try {   //adres van de host waarop de node gestart wordt
            location = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        startMulticastListener();
        System.out.println("Multicast listener ✓");
        startRMI();
        System.out.println("RMI ✓");
        fileServer = new FileServer(FILESERVER_PORT); //fileserver wordt opgestart
        System.out.println("Fileserver ✓");
        System.out.println("Waiting for replies...");
        sendBootstrapBroadcast();   //jezelf broadcasten over het netwerk
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
            System.out.println("Failed to send bootstrap broadcast. Aborting...");
            System.exit(1);
        }
    }

    /**
     * Deze node kan eender welke node verwijderen uit de nameServer
     *
     * @param hash de id van te verwijderen node
     */
    public void deleteNode(int hash) {
        try {
            if (!nameServer.removeNode(hash))
                System.out.println("Deze node bestaat niet");
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    /**
     * deze node wordt verwijderd uit de nameserver en sluit af
     * bestanden die hier gerepliceerd staan, worden gerepliceeerd naar de vorige node
     * Van de lokale bestanden wordt de eigenaar verwittigd of de downloadlocaties aangepast
     */
    public void exit() {
        ArrayList<String> downloads;
        FileEntry fileEntry;
        NodeInterface node;

        System.out.println("Leaving the network and updating my neighbours...");
        try {
            if(previousNode != -1 && nextNode != -1) { //Eerst nakijken of node wel volledig is opgestart
            getNode(previousNode).setNextNode(nextNode);//naar de previous node het id van de next node sturen
            getNode(nextNode).setPreviousNode(previousNode);//naar de next node het id van de previous node sturen
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if(previousNode != -1 && nextNode != -1) { //Eerst nakijken of node wel volledig is opgestart
            //bestanden die hier gerepliceerd staan, repliceren naar de vorige node
            System.out.println("replicating my files to previous node...");

            for (HashMap.Entry<String, FileEntry> entry : replicatedFiles.entrySet()) {
                fileEntry = entry.getValue();           //elke bestandsfiche een voor een aflopen
                try { //entry aanpassen
                    fileEntry.setReplicated(nameServer.getAddress(previousNode));
                    fileEntry.setOwner(nameServer.getAddress(previousNode));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


                try { //bestandsfiche doorsturen naar lokale node
                    node = getNode(fileEntry.getLocal());   //node waar het bestand lokaal staat
                    if (!node.changeLocalEntry(fileEntry.getFileName(), fileEntry))
                        System.out.println("bestand kan niet aangepast worden want het bestaat niet...");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                //node waar het bestand naar gerepliceerd wordt

                //file repliceren naar de vorige node
                try {
                    node = getNode(previousNode);
                    node.replicateNewFile(fileEntry);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                sendFile(previousNode, fileEntry.getFileName(), REPLICATED_DIRECTORY); // bestand doorsturen naar de vorige node
            }


        //Van de lokale bestanden wordt de eigenaar verwittigd of de downloadlocaties aangepast
        System.out.println("warning owner of local files...");
        for(HashMap.Entry<String, FileEntry> entry: localFiles.entrySet())
        {
            fileEntry = entry.getValue();
            try {
                node = getNode(fileEntry.getOwner()); //eigenaar van het bestand
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            downloads=fileEntry.getDownloadLocations();

            /*if(downloads.isEmpty()) //nog nergens gedownload geweest
                try {
                    if(!node.deleteReplicatedFile(fileEntry.getFileName()))
                        System.out.println("bestand kan niet verwijderd worden want het bestaat niet");
                        //TODO: bestand lokaal van de schijf verwijderen
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            //else
            // TODO: downloadlocaties updaten dat de lokale node weg is?
            //Is de lokale node dan ook een downloadlocatie?*/
        }
        //TODO: Ivan: laatste bestanden worden niet verzonden bij een exit, methode replicatenewfile wordt wel aangeroepen maar het bestand zelf wordt niet verzonden over tcp
        }
        if(nameServer != null)
        deleteNode(hashName(name));                     //node verwijderen uit de nameserver
        System.exit(0);                                 //systeem afsluiten
    }

    @Override
    public boolean deleteReplicatedFile(String naam) throws RemoteException
    {
        if(replicatedFiles.containsKey(naam)) {
            FileEntry entry = replicatedFiles.get(naam);
            entry.removeDownloadLocation(location);
            replicatedFiles.remove(naam);
            updateEntryAllNodes(entry);
            //TODO: verwijderen van harde schijf
            return true;
        }else
            return false;
    }

    /**
     * haalt fileEntry uit de remote replicated folder op
     * @param fileName fileName van de entry die je wil hebben
     * @return fileEntry, of null
     * @throws RemoteException
     */
    @Override
    public FileEntry getRemoteFileEntry(String fileName) throws RemoteException {
        return replicatedFiles.get(fileName);
    }

    @Override
    public void replicateNewFile(FileEntry entry)
    {
        System.out.println("bestand met naam " + entry.getFileName() + " wordt naar mij gerepliceerd...");
        String name = entry.getFileName();
        if (!localFiles.containsKey(name)) //als het bestand nog niet lokaal bestaat
        {
            if (!entry.getLocalIsOwner()) {
                entry.setOwner(location);
                System.out.println("local is niet de owner van " + entry.getFileName());
            }
            entry.setReplicated(location);
            entry.addDownloadLocation(location);
            replicatedFiles.put(name, entry);
            System.out.println("bestand met naam " + entry.getFileName() + " wordt gerepliceerd naar mij en heeft als owner: " + entry.getOwner() + " en heeft als hash " + entry.getHash());
            System.out.println("bestand succescol gerepliceerd");
        } else { //bestand bestaat lokaal en wordt gerepliceerd naar de vorige
            entry.setOwner(location);
            entry.setLocalIsOwner(true);
            if (previousNode == myHash){ //maar 1 node in netwerk
                replicatedFiles.put(name, entry);
                System.out.println("Er zit maar 1 node in het netwerk, bestand wordt naar mezelf gerepliceerd...");
            }else {
                try {
                    NodeInterface node = getNode(previousNode);
                    System.out.println("bestand wordt gerepliceerd naar vorige node want het staat lokaal bij mij");
                    node.replicateNewFile(entry);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            sendFile(previousNode, entry.getFileName(), LOCAL_DIRECTORY);
        }updateEntryAllNodes(entry);
    }

    /**
     * Start de multicast listener op. Ontvang multicasts van andere nodes en worden hier behandeld
     */
    private void startMulticastListener() {
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(InetAddress.getByName(GROUP));
        } catch (IOException e) {
            System.out.println("Failed to start multicast, perhaps already running...? Aborting...");
            System.exit(1);
        }

        /**
         * Finalize socket so thread is willing to use it.
         */
        final MulticastSocket finalMulticastSocket = multicastSocket;
        new Thread(new Runnable() {
            public void run() {
                int hash = 0;
                try {
                    while (true) {
                        byte[] buf = new byte[256];
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        finalMulticastSocket.receive(datagramPacket);
                        byte[] byteAddress = Arrays.copyOfRange(buf, 0, 4);
                        String address = InetAddress.getByAddress(byteAddress).getHostAddress();
                        String name = new String(Arrays.copyOfRange(buf, 4, 255)).trim();
                        hash = hashName(name);
                        NodeInterface node = getNode(address); //Vraag node op langs address want het kan zijn dat hij nog niet in de nameserver staat.
                        /**
                         * Ga eerst na of we de enigste node waren in het netwerk. Zo ja, zet vorige en volgende naar
                         * de nieuwe node, en zet die van de nieuwe node naar ons.
                         * https://gyazo.com/f0a9b650813f46d1b98ac63bb6b396fb
                         */
                        if (previousNode == myHash && nextNode == myHash) {
                            previousNode = hash;
                            nextNode = hash;
                            node.setNextNode(myHash);
                            node.setPreviousNode(myHash);
                            System.out.println("A second node has joined. I've set my previous and next node to him and updated him.");
                            checkOwnedFilesOnDiscovery();
                        }
                        /**
                         * Hierna gaan we na of de node tussen ons en één van onze buren ligt
                         */
                        else if ((myHash < hash && hash < nextNode) || (nextNode < myHash && (hash > myHash || hash < nextNode))) {
                            /**
                             * SITUATIE 1: (eerste deel van if-case)
                             * De node ligt tussen mij en mijn volgende buur. De nieuwe node is mijn volgende en ik ben
                             * de vorige van de nieuwe node. Ik zeg dit tegen de nieuwe node en pas mijn volgende aan.
                             *
                             * Methode checkOwnedFileOnDiscovery aanroepen.
                             */
                            /**
                             * SITUATIE 2: (tweede monstreuze deel van if-case)
                             * Ik zit aan het einde van de kring want mijn volgende node is lager dan mij.
                             * De nieuwe node ligt boven mij, of ligt onder mijn volgende (laagste) node. Ik licht
                             * de nieuwe node in over zijn buren en pas mijn volgende aan.
                             */
                            node.setPreviousNode(myHash);
                            node.setNextNode(nextNode);
                            System.out.printf("A node (%d) joined between me (%d) and my next neighbour (%d). Updating accordingly...\nWelcome %s!\n", hash, myHash, nextNode, name);
                            nextNode = hash;
                            checkOwnedFilesOnDiscovery();
                        } else if ((previousNode < hash && hash < myHash) || (previousNode > myHash && (hash < myHash || hash > previousNode))) {
                            /**
                             * De node ligt tussen mijn vorige buur en mij. Mijn vorige buur zal de nieuwe node
                             * over zijn nieuwe buren informeren. Ik pas enkel mijn vorige node aan.
                             */
                            System.out.printf("A node (%d) joined between my previous neighbour (%d) and me. Updating accordingly...\nWelcome %s!\n", hash, previousNode, name);
                            previousNode = hash;
                        } else if (hash == myHash) {
                            //System.out.printf("I joined the network.\n");
                        } else {
                            System.out.printf("A node (%d) joined but isn't between my previous or next neighbour.\nWelcome %s!\n", hash, name);
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
            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setPreviousNode(int hash) {
        this.previousNode = hash;
        checkWatchDir();
    }

    @Override
    public void setNextNode(int hash) {
        this.nextNode = hash;
        checkWatchDir();
    }

    @Override
    public void printMessage(String message) throws RemoteException {
        System.out.println(message);
    }

    public void checkWatchDir() {
        if(previousNode != -1 && nextNode != -1 && nameServer != null && watchDir == null) {
            System.out.println("Received both nodes and discovered nameserver, starting Watchdir...");
            try {
                watchDir = new WatchDir(LOCAL_DIRECTORY, false, this);//watchdir class op LOCAL_DIRECTORY, niet recursief, op deze node
            } catch (IOException e) {
                System.out.println("Failed to start watchdir, aborting...");
                //TODO: exit oproepen zodat nameserver ons ziet weggaan
                System.exit(1);
            }
            System.out.println("Watchdir ✓");
        }
    }

    /**
     * de methode die moet aangeroepen worden wanneer de communicatie met een Node mislukt is
     *
     * @param hash het id van de node waarmee de communicatie mislukt is
     */
    public void failure(int hash) {
        try {
            System.out.println("Detected failure from "+hash+".");
            RecoveryAgent agent = null;
            if(hash == previousNode) {
                agent = new RecoveryAgent(hash, this, true);
            }
            else
            {
                agent = new RecoveryAgent(hash, this, true);
            }
            int nextNode = nameServer.getNextNode(hash);
            int previousNode = nameServer.getPreviousNode(hash);
            getNode(previousNode).setNextNode(nextNode);//naar de previous node het id van de next node sturen
            getNode(nextNode).setPreviousNode(previousNode);//naar de next node het id van de previous node sturen
            deleteNode(hash);                                 //node verwijderen
            startAgent(agent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    //TODO: failure methode op de juiste plekken oproepen

    /**
     * dit is slechts een testmethode om de failure methode op te roepen.
     */
    public void sendPing() {
        try {
            Socket socket = new Socket(nameServer.getAddress(nextNode), PING_PORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            while (true) {
                dataOutputStream.writeUTF("ping");
                String pong = dataInputStream.readUTF();
                if (pong.equals("pong")) {
                    dataOutputStream.writeUTF("ping");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            failure(nextNode);
        }
    }

    /**
     * Testmethode om bij sendping methode te gaan, persoon die zal ontvangen moet eerst receiveping starten, persoon die zal senden moet dan sendping starten, kabel uittrekken van persoon die receiveping draait.
     */
    public void receivePing() {
        try {
            ServerSocket serverSocket = new ServerSocket(PING_PORT);
            Socket clientSocket = serverSocket.accept();
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            while (true) {
                String ping = dataInputStream.readUTF();
                if (ping.equals("ping")) {
                    dataOutputStream.writeUTF("pong");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Vraag een bestand op naar een andere node. De bestanden worden gezocht in de subfolder ./files en zullen op de
     * eigen node ook in deze map geplaatst worden.
     *
     * @param node      De hash van de node
     * @param filename  Naam van het bestand
     * @return          Of het bestand gevonden was of niet, of dat de node niet bestaat.
     */
    public boolean requestFile(int node, String filename) {
        try {
            return requestFile(nameServer.getAddress(node), filename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Verstuur een bestand naar een andere node. De bestanden worden gezocht in de subfolder ./files en zullen op de
     * destination ook in deze map geplaatst worden.
     *
     * @param node      Hash van de node
     * @param filename  Bestandsnaam
     * @return          Of dat de server het bestand successvol heeft ontvangen
     */
    public boolean sendFile(int node, String filename, Path pad) {
        try {
            return sendFile(nameServer.getAddress(node), filename, pad);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vraag een bestand op van een IP address. Kijk naar requestFile(int, String voor meer uitleg)
     *
     * @param address
     * @param filename
     * @return
     */
    public boolean requestFile(String address, String filename) {
        try {
            Socket socket = new Socket(address, FILESERVER_PORT);
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dataOutputStream.writeUTF("send");
            dataOutputStream.writeUTF(filename);
            FileOutputStream fileOutputStream = new FileOutputStream("."+File.separator+ REPLICATED_DIRECTORY + File.separator+filename);//TODO mogelijk is "."+file.seperator niet nodig.
            byte[] bytes = new byte[8192];
            int count;
            while ((count = dataInputStream.read(bytes)) > 0) {
                fileOutputStream.write(bytes, 0, count);
            }
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
            return true;

        } catch (IOException e) {
            return false;
            //e.printStackTrace();
        }
    }

    /**
     * Verstuur een bestand naar een andere node. De bestanden worden gezocht in de subfolder ./files en zullen op de
     * destination ook in deze map geplaatst worden.
     * @param address ip adres van de node
     * @param filename bestandsnaam
     * @param path pad naar de directory waar het bestand nu staat
     * @return  Of dat de server het bestand successvol heeft ontvangen
     */
    public boolean sendFile(String address, String filename, Path path) {
        try {
            Socket socket = new Socket(address, FILESERVER_PORT);
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dataOutputStream.writeUTF("receive");
            dataOutputStream.writeUTF(filename);
            FileInputStream fileInputStream = new FileInputStream("."+File.separator + path +File.separator+ filename);//TODO mogelijk is "."+file.seperator niet nodig.
            //System.out.println(path);
            byte[] bytes = new byte[8192];
            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                dataOutputStream.write(bytes, 0, count);
            }
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
            return true;

        } catch (IOException e) {
            return false;
            //e.printStackTrace();
        }
    }

    /**
     * Pas een bestandsfiche van een lokaal bestand aan
     * @param name de bestandsnaam
     * @param entry de nieuwe entry
     * @return true als het bestand bestaat, false als het niet bestaat
     */
    @Override
    public boolean changeLocalEntry(String name, FileEntry entry) throws RemoteException
    {
        if(localFiles.get(name) != null)
        {
            localFiles.put(name,entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Pas een entry van een replicated bestand aan
     * @param name de bestandsnaam
     * @param entry de nieuwe entry
     * @return true als het bestand bestaat, false als het niet bestaat
     */
    @Override
    public boolean changeReplicatedEntry(String name, FileEntry entry) throws RemoteException
    {
        if(replicatedFiles.get(name) != null)
        {
            replicatedFiles.put(name,entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Methode die wordt aangeroepen in de constructor om RMI op te starten
     */
    private void startRMI() {
        try {
            NodeInterface nodeInterface = (NodeInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("NodeInterface", nodeInterface);
            System.out.println("Started RMI. Ready for connections...");
        } catch (RemoteException | AlreadyBoundException e) {
            System.out.println("RMI not running or hasn't been restarted. Aborting...");
            System.exit(1);
        }
    }

    /**
     * Geeft de interface terug van een Node zodat we deze node via RMI kunnen bereiken
     * @param hash de hash van de gewenste Node
     * @return de interface die we gebruiken om RMI aan te roepen
     */
    public NodeInterface getNode(int hash) throws RemoteException {
            return getNode(nameServer.getAddress(hash));
    }

    /**
     * Geeft de interface terug van een Node zodat we deze node via RMI kunnen bereiken
     * @param IP het ip adres van de gewenste Node
     * @return de interface die we gebruiken om RMI aan te roepen
     */
    public NodeInterface getNode(String IP) throws RemoteException {
        String name = String.format("//%s/NodeInterface", IP);
        try {
            return (NodeInterface) Naming.lookup(name);
        } catch (NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setSize(String ip, int size) {
        if(size == -1) {
            System.out.println("Nameserver rejected our node because of duplicate entry. Quitting...");
            System.exit(1);
        } else {
            System.out.println("Nameserver ✓ (" + ip +")");
            connectToNameServer(ip);
            if (size == 1) {
                System.out.println("I'm the first node. I'm also the previous and next node. ");
                previousNode = myHash;
                nextNode = myHash;
                //getNode(nextNode).startAgent(new FileAgent());
                try {
                    watchDir = new WatchDir(LOCAL_DIRECTORY, false, this);//watchdir class op LOCAL_DIRECTORY, niet recursief, op deze node
                } catch (IOException e) {
                    System.out.println("Failed to start watchdir, aborting...");
                    //TODO: exit oproepen zodat nameserver ons ziet weggaan
                    System.exit(1);
                }
                System.out.println("Watchdir ✓");
            } else {
                System.out.printf("I'm not the first node (size is %d). Waiting for my next and previous node...\n", size);
            }
            checkWatchDir();
        }
    }

    /**
     * verbinding maken met de nieuwe nameserver
     * @param IP Ip van de nameserver
     */
    private void connectToNameServer(String IP) {
        System.out.println("Attempting to connect to NameServer");
        String rmiName = "//" + IP + "/NameServerInterface";
        try {
            nameServer = (NameServerInterface) Naming.lookup(rmiName);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
        System.out.println("Successfully connected to NameServer!");
    }

    /**
     * Method die door WatchDir opgeroepen wordt wanneer een event plaatsvindt
     * @param eventType type van event
     * @param fileName de filename, in String, met extensie
     */
    public void directoryChange(String eventType,String fileName) {
        String node;
        System.out.println("proberen om een event te behandelen");
        switch (eventType) {
            case "ENTRY_CREATE":
                try {
                    String owner = nameServer.getOwner(fileName);//bij fileserver opvragen op welke node dit bestand gerepliceerd moet worden IP krijgen we terug
                    FileEntry fileEntry = new FileEntry(fileName, location, owner, owner);
                    localFiles.put(fileName, fileEntry);
                    NodeInterface nodeInterface = getNode(owner);
                    nodeInterface.replicateNewFile(fileEntry);
                    if(!owner.equals(location)){
                        System.out.println("bestand verzenden aanroepen");
                        sendFile(owner, fileName, LOCAL_DIRECTORY);
                        System.out.println("bestand is verzonden naar replicated");
                    }
                    else{
                        System.out.println("bestand verzenden aanroepen");
                        sendFile(previousNode, fileName, LOCAL_DIRECTORY);
                        System.out.println("bestand is verzonden naar previous node");
                    }

                } catch (RemoteException e) {
                    System.out.println("RMI Exception bij replicatie. Node wordt beschouwd als gefaalt.");
                    try {
                        failure(nameServer.getOwnerHash(fileName));
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
                break;
            case "ENTRY_DELETE":
                /*
                  nog niet gespecifieerd in de opgave.
                 */
                break;
        }
    }

    /**
     * Wanneer een nieuwe node start moet de vorige node (deze node bijvoorbeeld) nagaan of
     * er bestanden zijn die hier gerepliceerd zijn, die dan naar de nieuwe node gerepliceerd
     * moeten worden. Zoja, bestandsfiche updaten en via TCP doorsturen
     */
    public void checkOwnedFilesOnDiscovery() {
        //localfiles afgaan
        Iterator iterator = localFiles.entrySet().iterator();
        while (iterator.hasNext()) {
            //Controleer eerst ownership
            //Owner? kijk of nieuwe node owner moet worden
            Map.Entry<String, FileEntry> pair = (Map.Entry<String, FileEntry>) iterator.next();
            FileEntry valueOfEntry = pair.getValue();
            if (valueOfEntry.getOwner().equals(location)) { //ik ben zelf de eigenaar
                if (valueOfEntry.getHash() >= nextNode) {   //bestand moet gerepliceerd worden naar de nieuwe node
                    //de nieuwe node wordt eigenaar (=nextnode)
                    //wordt zelf downloadlocatie?
                    try {
                        if(valueOfEntry.getHash()>myHash && myHash < nextNode && previousNode == nextNode){
                            valueOfEntry.setOwner(location);
                        }else {
                            valueOfEntry.setOwner(nameServer.getAddress(nextNode));
                        }
                        valueOfEntry.setReplicated(nameServer.getAddress(nextNode));
                        //TODO mogelijk onderstaande lijn uit comment halen.
                        //valueOfEntry.addDownloadLocation(location); eigen IP aan downloadlocations toevoegen?
                        NodeInterface nodeInterface = getNode(nextNode);
                        nodeInterface.replicateNewFile(valueOfEntry);
                        sendFile(nextNode, valueOfEntry.getFileName(),LOCAL_DIRECTORY);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (previousNode ==nextNode) //maar 2 nodes in het netwerk
                {
                    if(valueOfEntry.getHash()<myHash)   //andere node in het netwerk krijgt het bestand gerepliceerd en wordt eigenaar
                        try {
                            valueOfEntry.setOwner(nameServer.getAddress(nextNode));
                            valueOfEntry.setLocalIsOwner(false);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    else {  //ik repliceer het bestand naar de vorige node maar blijf zelf wel eigenaar want de hash van het document verwijst naar mezelf
                        valueOfEntry.setOwner(location);
                        valueOfEntry.setLocalIsOwner(true);
                    }
                    try {
                        valueOfEntry.setReplicated(nameServer.getAddress(nextNode));
                        NodeInterface nodeInterface = getNode(nextNode);
                        nodeInterface.replicateNewFile(valueOfEntry);
                        sendFile(nextNode, valueOfEntry.getFileName(),LOCAL_DIRECTORY);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        iterator = replicatedFiles.entrySet().iterator();
        //replicatedfiles afgaan
        while (iterator.hasNext()) {
            Map.Entry<String, FileEntry> pair = (Map.Entry<String, FileEntry>) iterator.next();
            FileEntry valueOfEntry = pair.getValue();
            //Owner?
            if (valueOfEntry.getOwner().equals(location)) {
                //kijk of nieuwe node beter geschikt is voor bestanden te repliceren
                if (valueOfEntry.getHash() >= nextNode) {
                    //de nieuwe node wordt eigenaar (=nextnode)
                    //wordt zelf downloadlocatie
                    try {
                        if(valueOfEntry.getHash()>myHash && myHash < nextNode && previousNode == nextNode){
                            valueOfEntry.setOwner(location);
                        }else {
                            valueOfEntry.setOwner(nameServer.getAddress(nextNode));
                        }
                        valueOfEntry.setReplicated(nameServer.getAddress(nextNode));
                        valueOfEntry.addDownloadLocation(location);
                        NodeInterface nodeInterface = getNode(nextNode);
                        nodeInterface.replicateNewFile(valueOfEntry);
                        sendFile(nextNode, valueOfEntry.getFileName(), REPLICATED_DIRECTORY);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //Geen Owner?
                if (valueOfEntry.getHash() >= nextNode) {
                    //repliceer naar volgende node, maar deze wordt geen eigenaar.
                    //Wordt zelf downloadlocatie.
                    try {
                        if(valueOfEntry.getHash()>myHash && myHash < nextNode && previousNode == nextNode){
                            valueOfEntry.setOwner(location);
                        }else {
                            valueOfEntry.setOwner(nameServer.getAddress(nextNode));
                        }
                        valueOfEntry.addDownloadLocation(location);
                        NodeInterface nodeInterface = getNode(nextNode);
                        nodeInterface.replicateNewFile(valueOfEntry);
                        sendFile(nextNode, valueOfEntry.getFileName(), REPLICATED_DIRECTORY);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }


        }
    }


    public int getMyHash(){
        return myHash;
    }

    @Override
    public void startAgent(AgentInterface agent)
    {
        System.out.println("agent is gestart op deze node");
        class Temp implements Runnable {
            Node node;
            Temp(Node node) {this.node=node;}
            public void run() {
                agent.setCurrentNode(node);     //huidige node instellen bij de agent
                Thread t = new Thread(agent);   //nieuwe thread opstarten waar de agent in loopt
                t.start();
                try {
                    t.join();                   //wachten tot de agent klaar is met lopen
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if(!agent.isFinished())
                        getNode(nextNode).startAgent(agent);//agent starten op de volgende Node
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        Thread t = new Thread(new Temp(this));
        t.start();
        System.out.println("agent is doorgegeven naar volgende node");
    }

    public HashMap getFileList()
    {
        return fileList;
    }

    public void setFileList(HashMap<String,Boolean> list)
    {
        fileList = list;
    }

    /**
     * deze methode zorgt voor het aanvragen van een lock op een bestand
     * @param filename de bestandsnaam waarop de lock moet aangevraagd worden
     * @return true als de actie succescol was, false als de bestandsnaam niet bestaat
     */
    private boolean requestFileLock(String filename)
    {
        if(fileList.containsKey(filename)) {
            fileList.put(filename, true);
            return true;
        }else
            return false;
    }

    /**
     * lock op een bestand mag opgeheven worden
     * @param filename naam van het bestand waarvan de lock opgeheven wordt
     * @return true als het gelukt is, false als het bestand niet gelocked was
     */
    private boolean releaseFileLock(String filename)
    {
        if(lockedFiles.contains(filename))
        {
            lockedFiles.remove(filename);
            fileList.put(filename,false);
            return true;
        }else
            return false;
    }

    /**
     * wordt opgeroepen door de agent om aan te geven dat een bestand gelocked is voor deze node
     * @param filename naam van gelockte bestand
     */
    protected void approveFileLock(String filename)
    {
        lockedFiles.add(filename);
    }

    /**
     * roep deze methode aan om een entry aan te passen op alle nodes waar ze aanwezig is
     * @param entry de aangepaste entry
     */
    private void updateEntryAllNodes(FileEntry entry)
    {
        String naam = entry.getFileName();
        for(String IP: entry.getDownloadLocations()) {
            try {
                if (getNode(IP).changeReplicatedEntry(naam, entry))
                    System.out.println("replicated entry " + naam + " is aangepast op node " + IP);
                else
                    System.out.println("ERROR: replicated entry " + naam + " bestaat niet op node " + IP);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String IP=entry.getLocal();
        try {
            if(getNode(IP).changeLocalEntry(naam,entry))
                System.out.println("local entry " + naam + " is aangepast op node " + IP);
            else
                System.out.println("ERROR: local entry " + naam + " bestaat niet op node " + IP);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public NameServerInterface getNameServer() {
        return nameServer;
    }

    public int getPreviousNode() {
        return this.previousNode;
    }

    public int getNextNode() {
        return this.nextNode;
    }

    /**
     * deze methode geeft een bestand terug dat ergens in het netwerk staat
     * @param filename de naam van het bestand dat moet opgezocht worden
     * @return het bestand
     */
    public File displayFile(String filename)
    {
        System.out.println("requesting filelock...");
        while(!requestFileLock(filename));          // lock blijven aanvragen tot bestand in onze filelist staat
        System.out.println("filelock is requested");
        while(!lockedFiles.contains(filename)) {}   //wacht tot bestand aan ons gegeven wordt
        System.out.println("bestand "+filename+" is gelocked ");
        NodeInterface node = null;
        ArrayList<String> downloadLocations = null;
            try {
                String owner = nameServer.getOwner(filename);
                node = getNode(owner);
                HashMap<String, FileEntry> files = node.getReplicatedFiles();
                if(files.containsKey(filename)) {
                    downloadLocations = files.get(filename).getDownloadLocations();
                } else {
                    files = node.getLocalFiles();
                    if(files.containsKey(filename)) {
                        downloadLocations = files.get(filename).getDownloadLocations();
                    } else {
                        System.out.println("ERROR: bestand bestaat nergens op de owner");
                        return null;
                    }
                }
                Random rand = new Random();
                String IP = downloadLocations.get(rand.nextInt(downloadLocations.size()));
                requestFile(IP,filename);       //file wordt opgeslagen in eigen replicated directory
                releaseFileLock(filename);
                return new File(REPLICATED_DIRECTORY + File.separator + filename);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        return null;
    }

    @Override
    public HashMap<String, FileEntry> getReplicatedFiles() {
        return replicatedFiles;
    }

    @Override
    public HashMap<String, FileEntry> getLocalFiles() {
        return localFiles;}
}