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
            Het probleem is dat fileAgent moet weten welke bestanden er nog beschikbaar
            zijn in het netwerk nadat een node exit of failed. Het is echter gegarandeerd
            dat een bestand altijd minstens 2 keer aanwezig is in het netwerk? Dit houdt
            in dat er altijd een entry mag bestaan (zei het zonder lock owner) met als key
            de naam van een bestand eens dit bestand ooit in het netwerk is gekomen.
             */

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
            Vermits de huidige node in zijn outdated fileList nog geen notie
            heeft van zijn eigen nieuwe bestanden kan hij daar onmogenlijk al
            een lock hebben aangevraagd.
             */
            HashMap<String, Boolean> currentNodeFileList = currentNode.getFileList();
            for (Map.Entry<String, Boolean> entry : currentNodeFileList.entrySet()) {
                /*
                We gaan de oude lijst van de huidige node af. Als de huidige node een lock heeft aangevraagd voor een
                bestand kijken we na of er niet reeds een lock bestond voor dat bestand. In dat geval wordt de eigenaar
                van de lock teruggegeven want: als de key van de lockedFilesMap gemapped is naar een null pointer (er was geen entry)
                of nog niet gemapped is naar een waarde wordt de nieuwe waarde toegekend aan de key en zal de methode null
                teruggeven, zoniet wordt de huidige waarde teruggegeven (dit is dan de hash van de eigenaar van een lock).
                We slaan de waarde van de eigenaar van een lock op ter controle. Echter vermits de default waarde -1 is
                zal er nooit null worden teruggegeven. De return is dus altijd: -1 (geen bestaande locks), ofwel de hash
                van de huidige eigenaar van lock. Er kunnen ook onmogenlijk nieuwe entries bijkomen want de node heeft
                zoals gezegd in zijn outdated lijst nog geen notie van nieuwe files in het netwerk. We gebruiken de methode
                putIfAbsent dus enkel ter controle. Als de huidige owner -1 is kunnen we enkel in de else branch terecht komen.
                In de else branch zal de -1 hash dan vervangen worden door de hash van de huidige node als deze een lock
                heeft aangevraagd voor het beschouwde bestand.
                 */
                int ownerOfLock = lockedFilesMap.putIfAbsent(entry.getKey(), entry.getValue() ? currentNode.getMyHash() : -1);
                //boolean currentIsFirst = currentNode.getMyHash() < currentNode.getPreviousNode();
               // boolean lastNodeIsGone = currentIsFirst && ownerOfLock > currentNode.getPreviousNode();
                boolean isReplaced = false;
                //if ((lastNodeIsGone || (currentNode.getMyHash() > ownerOfLock)) && (ownerOfLock != -1)) {
                if (!currentNode.nodeIsPresent(ownerOfLock)) {
                    //TODO: print statements verwijderen na testen
                    System.out.println("The file agent noticed node with hash: "+ownerOfLock+" has exited or failed");
                    System.out.println("The lock of "+ownerOfLock+" on "+entry.getKey()+" has been released");
                    /*
                    Heeft de huidige node een lock aangevraagd voor een bestand? Zoja, vervang de vorige eigenaar
                    van de lock door de hash van de huidige node. Indien nee, vervang de hash van de(gefailde of geexitte)
                    eigenaar door de default value -1 want deze reageert niet meer.De speciale versie van de volgende
                    methode werd enkel gebruikt om na te gaan (a.d.h.v. de return boolean) of een file lock werd toegestaan
                    voor het bestand. Er werd immers reeds gecontroleerd of er iemand het bestand reeds had gelocked,
                    zoniet is er nooit een probleem en komen we nooit in deze branch.
                     */
                    isReplaced = lockedFilesMap.replace(entry.getKey(), ownerOfLock, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (isReplaced && entry.getValue())
                        currentNode.approveFileLock(entry.getKey());
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
                    isReplaced = lockedFilesMap.replace(entry.getKey(), isOwnerOfLock ? currentNode.getMyHash() : -1, entry.getValue() ? currentNode.getMyHash() : -1);
                    if (isReplaced && entry.getValue())
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
