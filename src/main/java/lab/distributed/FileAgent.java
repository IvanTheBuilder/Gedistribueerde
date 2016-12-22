package lab.distributed;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Robbe on 6/12/2016.
 */
public class FileAgent implements AgentInterface, Serializable {
    /**
     * LockedFilesMap bevat alle beschikbare bestanden en per bestand de hash van een node
     * die er een lock op heeft geplaatst. Indien er geen enkele node een lock heeft op een
     * beschikbaar bestand is de value -1.
     */
    private HashMap<String, Integer> lockedFilesMap = new HashMap<>();
    private Node currentNode;

    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     * Deze methode gaat per node na of er nieuwe bestanden beschikbaar zijn. Zoja zullen deze
     * worden toegevoegd aan een lijst. Elke node bezit een kopie van deze lijst en kan een
     * lock aanvragen voor een bestand door een flag te zetten. De agent gaat per node in de
     * kring na of er een lock werd aangevraagd en zal het bestand locken als dit nog niet het geval is.
     * Elke ronde moet een node zijn lock bevestigen.
     *
     * @throws NullPointerException Opgegooid als er nog niet werd aangegeven op welke node de agent draait.
     */
    @Override
    public void run() throws NullPointerException {
        if (currentNode == null) throw new NullPointerException("Reference to currentNode is null");
        else {

            /*
            We gaan eerst de lijst van locale files op de huidige node af om te kijken of
            er nieuwe bestanden zijn bijgekomen, zoja voegen we deze toe aan de lockedFilesMap
            en zetten we de waarde op -1 om aan te geven dat er geen enkele node een lock
            heeft op dit bestand. Als er al een entry bestaat voor het bestand wordt
            er niets aangepast.
             */
            HashMap<String, FileEntry> currentNodeLocalFiles = currentNode.getLocalFiles();
            for (Map.Entry<String, FileEntry> entry : currentNodeLocalFiles.entrySet()) {
                lockedFilesMap.putIfAbsent(entry.getKey(), -1);
            }

            /*
            Vervolgens gaan we de outdated fileList van de huidige node opvragen.
            We gaan na of er een lock werd aangevraagd voor een bepaald bestand.
            Op het einde geven we een nieuwe lijst door aan de huidige node.
            */
            HashMap<String, Boolean> currentNodeFileList = currentNode.getFileList();
            for (Map.Entry<String, Boolean> entry : currentNodeFileList.entrySet()) {
                /*
                We gaan de oude lijst van de huidige node af. We gaan na of er reeds een lock bestaat voor het bestand.
                De return is altijd: -1 (geen bestaande locks), ofwel de hash van de huidige eigenaar van lock. Er kunnen
                ook onmogenlijk nieuwe entries bijkomen want de node heeft nog geen notie van nieuwe files. Als de huidige
                owner -1 is kunnen we enkel in de else branch terecht komen. Als de eigenaar van een lock niet meer in het
                netwerk zit komen we in de if branch terecht.
                 */
                int ownerOfLock = lockedFilesMap.get(entry.getKey());
                boolean isReplaced = false;
                if (!currentNode.nodeIsPresent(ownerOfLock) && (ownerOfLock != -1)) {
                    //TODO: print statements verwijderen na testen
                    System.out.println("The file agent noticed node with hash: "+ownerOfLock+" has exited or failed");
                    System.out.println("The lock of "+ownerOfLock+" on "+entry.getKey()+" has been released");
                    /*
                    Heeft de huidige node een lock aangevraagd voor een bestand? Zoja, vervang de vorige eigenaar
                    van de lock door de hash van de huidige node. Indien nee, vervang de hash van de verdwenen
                    eigenaar door de default value -1. Als de huidige node een lock wou en de eigenaar van die
                    lock is er niet meer dan krijgt de huidige node de lock.
                     */
                    isReplaced = lockedFilesMap.replace(entry.getKey(), ownerOfLock, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (isReplaced && entry.getValue())
                        currentNode.approveFileLock(entry.getKey());
                }
                else {
                    /*
                    Dit is de default branch. In het geval dat iemand een lock aanvraagd en er al iemand een
                    lock heeft dan wordt er niets aangepast, tenzij hij zelf lockowner was. Dan kijken we of
                    de huidige node zijn lock heeft vrijgegeven en schrijven we terug -1. Als de huidige node
                    geen lock heeft aangevraagd voor een bestand verandert er niets en wordt de waarde -1
                    opnieuw geschreven.
                     */
                    boolean isOwnerOfLock = lockedFilesMap.get(entry.getKey()) == currentNode.getMyHash();
                    isReplaced = lockedFilesMap.replace(entry.getKey(), isOwnerOfLock ? currentNode.getMyHash() : -1, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (isReplaced && entry.getValue())
                        currentNode.approveFileLock(entry.getKey());
                }
            }

            /*
            We geven een nieuwe lijst door aan de node. Als de huidige node een lock heeft op een
            bestand dan laten we deze flag staan in de nieuwe lijst. We voegen ook nieuwe bestanden
            toe aan deze lijst.
             */
            HashMap<String, Boolean> newFileList = new HashMap<>();
            newFileList.putAll(currentNode.getFileList());
            for (Map.Entry<String, Integer> entry : lockedFilesMap.entrySet()) {
                newFileList.putIfAbsent(entry.getKey(),false);
            }
            currentNode.setFileList(newFileList);
        }
        currentNode = null;
    }

    /**
     * Deze methode zet de "currentNode" waarop de agent draait.
     *
     * @param node de node waarop de agent zich momenteel bevindt.
     */
    @Override
    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }
}
