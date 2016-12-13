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
     * Elke ronde moet een node zijn lock bevestigen anders wordt de lock vrijgegeven en
     * wordt er vanuit gegaan dat de vorige eigenaar gefailed is. Dit zal door de huidige
     * node worden doorgegeven aan de server via de voorziene failure methode.
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
                We gaan de oude lijst van de huidige node af. Als de huidige node een lock heeft aangevraagd voor een
                bestand kijken we na of er niet reeds een lock bestond voor dat bestand. In dat geval wordt de eigenaar
                van de lock teruggegeven want: als de key van de lockeFilesMap gemapped is naar een null pointer (er was geen entry)
                of nog niet gemapped is naar een waarde wordt de nieuwe waarde toegekend aan de key en zal de methode null
                teruggeven, zoniet wordt de huidige waarde teruggegeven (dit is dan de hash van de eigenaar van een lock).
                We slaan de waarde van de eigenaar van een lock op ter controle.
                 */
                int ownerOfLock = lockedFilesMap.putIfAbsent(entry.getKey(), entry.getValue() ? currentNode.getMyHash() : -1);
                boolean isFirst = currentNode.getNextNode() > currentNode.getPreviousNode();
                boolean lockApproved = false;
                if ((isFirst ? (currentNode.getMyHash() < ownerOfLock) : (currentNode.getMyHash() > ownerOfLock)) && (ownerOfLock != -1)) {
                    /*
                    Heeft de huidige node een lock aangevraagd voor een bestand? Zoja, vervang de vorige eigenaar
                    van de lock want deze reageert niet meer en roep vervolgens de failure methode aan van de
                    huidige node door de vorige eigenaar van de lock mee te geven als argument. De speciale versie
                    van de volgende methode werd enkel gebruikt om na te gaan (a.d.h.v. de return boolean) of
                    een file lock werd toegestaan voor het bestand. Er werd immers reeds gecontroleerd of er iemand
                    het bestand reeds had gelocked, zoniet is er nooit een probleem en komen we nooit in deze branch.
                     */
                    lockApproved = lockedFilesMap.replace(entry.getKey(), -1, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (lockApproved)
                        currentNode.approveFileLock(entry.getKey());
                    currentNode.failure(ownerOfLock);
                }
                /*
                Deze branch is (onder andere) altijd geldig als u zelf een lock bezit op een bestand.
                Ook als de huidige node een lock heeft maar deze wil vrijgeven komen we hier terecht.
                 */
                else {
                    /*
                    In het geval dat er nog niemand een lock had op een bestand (de oude waarde
                    in de entry = -1) zal deze vervangen worden door een nieuwe waarde als de huidige node
                    een lock heeft aangevraagd voor dit bestand. In het geval dat iemand een lock aanvraagd
                    en er al iemand een lock heeft dan wordt er niets aangepast, tenzij hij zelf eigenaar van
                    een lock was (test of vorige waarde van de hash gelijk is aan die van de huidige node)
                    wordt er nagegaan of de node de lock heeft vrijgegeven en zal de waarde -1 worden geschreven.
                    Ook als de huidige node geen lock heeft aangevraagd voor een bestand verandert er niets
                    en wordt de waarde -1 opnieuw geschreven.
                     */
                    boolean isOwnerOfLock = lockedFilesMap.get(entry.getKey()) == currentNode.getMyHash();
                    lockApproved = lockedFilesMap.replace(entry.getKey(), isOwnerOfLock ? currentNode.getMyHash() : -1, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (lockApproved)
                        currentNode.approveFileLock(entry.getKey());
                }
            }

            /*
            We geven een nieuwe lijst door aan de node. Als de huidige node een lock heeft op een
            bestand dan laten we deze flag staan in de nieuwe lijst door te controleren of de
            hash van de eigenaar van een lock gelijk is aan die van de huidige node en deze
            boolean waarde in de nieuwe lijst te zetten.
             */
            HashMap<String, Boolean> newFileList = new HashMap<>();
            for (Map.Entry<String, Integer> entry : lockedFilesMap.entrySet()) {
                newFileList.put(entry.getKey(), entry.getValue() == currentNode.getMyHash());
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
