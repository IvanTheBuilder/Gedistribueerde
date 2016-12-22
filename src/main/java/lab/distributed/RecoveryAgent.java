package lab.distributed;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Created by Joost on 6/12/2016.
 */
public class RecoveryAgent implements AgentInterface, Serializable{
    String startingNodeLocation = null;
    Node currentNode = null;
    boolean justStarted = true;
    int hashFailedNode;
    boolean failedNodeWasPreviousNode;
    int hashStartingNode;
    boolean finished = false;
    /**
     * Constructor voor een recovery agent. Voor goede werking let op volgende:
     * eerst de recoveryAgent initialiseren
     * dan Server notifyen en previous en next node aanpassen
     * tot slot recoveryAgent starten.
     *
     *
     * Voor info omtrent wat de RecoveryAgent juist doet, check commentaar boven run()
     * @param hashFailedNode De hash van de falende node
     * @param startingNode De node die de failure Agent opstart
     */
    public RecoveryAgent(int hashFailedNode, Node startingNode, boolean failedNodeWasPreviousNode){
        System.out.println("recoveryagent aangemaakt");
        this.hashFailedNode = hashFailedNode;
        this.startingNodeLocation = startingNode.getLocation();
        hashStartingNode = startingNode.getMyHash();
        this.currentNode = startingNode;
        this.failedNodeWasPreviousNode = failedNodeWasPreviousNode;
        System.out.println("Recovery agent aangemaakt!");
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    /*
            De Node die deze thread start moet de server laten weten dat de node gefaald is.

            Run methode van de RecoveryAgent
            lijst van lokale en replicated bestanden van de huidige node wordt overlopen.
            Reken hash uit van bestandsnamen en vraag aan server waar het bestand terecht komt.
            Als deze vgl opgaat: hashBestand >= failedNode en hashDieServerTerugGeeft < failedNode
                    a) bepaal nieuwe eigenaar. Dit is hashDierServerTerugGeeft, als bovenstaande vgl opgaat.
                    Stuur bestand door naar de nieuwe eigenaar.
                    Als deze node nog geen eigenaar is en niet over het bestand beschikt (replicated)
                    => update daar de bestandsfiche, met als eerste downloadlocatie deze node.
                    b) bepaal nieuwe eigenaar. Dit is hashDierServerTerugGeeft normaalgezien.
                    Als deze voor dit bestand al eigenaar is, update dan de downloadlocatie.
            Sluit af als huidige node gelijk is aan de startende node.
         */
    @Override
    public void run() {

        //indien de node niet net begonnen is, en de currentNode = startingNode, dan sluit de recoveyAgent af.
        if (!justStarted && currentNode.getLocation().equals(startingNodeLocation)) {
            finished = true;
        }

        HashMap<String, FileEntry> localFilesToCheck = currentNode.getLocalFiles();//methode moet nog worden aangemaakt in Node class
        HashMap<String, FileEntry> replicatedFileToCheck = currentNode.getReplicatedFiles();//methode moet nog worden aangemaakt in Node class
        NameServerInterface nameServerInterface = currentNode.getNameServer();//methode moet nog worden aangemaakt in Node class
        //Eerst de local files checken, of er al dan niet een bestand op een andere node terecht moet komen, dit is voor het normaal geval waarbij het bestand niet naar jezelf zou repliceren
        for (String key : localFilesToCheck.keySet()) {
            try {
                String IPofSupposedOwner = nameServerInterface.getOwner(key);
                int hashOfSupposedeOwner = currentNode.hashName(IPofSupposedOwner);
                int hashOfFile = currentNode.hashName(key);
                System.out.println("bestand behandelen:"+key);
                if (hashOfFile >= hashFailedNode && hashOfSupposedeOwner < hashFailedNode) {
                    NodeInterface newOwner = currentNode.getNode(IPofSupposedOwner);
                    FileEntry fileEntry = newOwner.getRemoteFileEntry(key);
                    //newOwner heeft het bestand nog niet staatn, maak een nieuwe entry aan en repliceer.
                    if (fileEntry == null) {
                        FileEntry newFilEntry = new FileEntry(key, InetAddress.getLocalHost().getHostAddress(), IPofSupposedOwner, IPofSupposedOwner);
                        newFilEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.replicateNewFile(newFilEntry);
                    } else if (!fileEntry.getOwner().equals(IPofSupposedOwner)) {
                        //Entry is al replicated, maar de node is geen eigenaar
                        FileEntry updatedFileEntry = localFilesToCheck.get(key);
                        updatedFileEntry.putOwner(IPofSupposedOwner);
                        updatedFileEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.changeReplicatedEntry(key, updatedFileEntry);
                    } else if (fileEntry.getOwner().equals(IPofSupposedOwner)) {
                        //het bestand bestaat al, en de supposednewOwner is al eigenaar door mijn hiervoor genomen acties
                        FileEntry updatedFileEntry = newOwner.getRemoteFileEntry(key);
                        updatedFileEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.changeReplicatedEntry(key, updatedFileEntry);
                    }
                } else if (hashOfSupposedeOwner == currentNode.getMyHash() && failedNodeWasPreviousNode) {
                    //wanneer een bestand naar jezelf zou repliceren, en de failed node was je previousnode, dan moet je gaan repliceren naar je nieuwe previousnode
                    //omdat deze al staat ingesteld (zie notes bij constructor) kan je dit gewoon doen.
                    int previousNodeHash = currentNode.getPreviousNode();
                    FileEntry newFileEntry = new FileEntry(key, InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), nameServerInterface.getAddress(previousNodeHash));
                    NodeInterface previousNode = currentNode.getNode(previousNodeHash);
                    previousNode.replicateNewFile(newFileEntry);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        //Ook replicated files worden afgegaan, zodat de downloadlocaties bij de nieuwe owner heropgebouwd worden. De check of failed node de previousnode is is hier niet nodig, want het gaat al om replicated files.
        for (String key : replicatedFileToCheck.keySet()) {
            try {
                String IPofSupposedOwner = nameServerInterface.getOwner(key);
                int hashOfSupposedeOwner = currentNode.hashName(IPofSupposedOwner);
                int hashOfFile = currentNode.hashName(key);
                System.out.println("bestand behandelen:"+key);
                if (hashOfFile >= hashFailedNode && hashOfSupposedeOwner < hashFailedNode) {
                    NodeInterface newOwner = currentNode.getNode(IPofSupposedOwner);
                    FileEntry fileEntry = newOwner.getRemoteFileEntry(key);
                    //newOwner heeft het bestand nog niet staatn, maak een nieuwe entry aan en repliceer.
                    if (fileEntry == null) {
                        FileEntry newFilEntry = new FileEntry(key, InetAddress.getLocalHost().getHostAddress(), IPofSupposedOwner, IPofSupposedOwner);
                        newFilEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.replicateNewFile(newFilEntry);
                    } else if (!fileEntry.getOwner().equals(IPofSupposedOwner)) {
                        //Entry is al replicated, maar de node is geen eigenaar
                        FileEntry updatedFileEntry = localFilesToCheck.get(key);
                        updatedFileEntry.putOwner(IPofSupposedOwner);
                        updatedFileEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.changeReplicatedEntry(key, updatedFileEntry);
                    } else if (fileEntry.getOwner().equals(IPofSupposedOwner)) {
                        //het bestand bestaat al, en de supposednewOwner is al eigenaar door mijn hiervoor genomen acties
                        FileEntry updatedFileEntry = newOwner.getRemoteFileEntry(key);
                        updatedFileEntry.addDownloadLocation(InetAddress.getLocalHost().getHostAddress());
                        newOwner.changeReplicatedEntry(key, updatedFileEntry);
                    }
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            finally {
                justStarted = false;
                currentNode = null;
            }
            justStarted = false;
        }
        currentNode = null;
    }

    @Override
    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }
}
