package lab.distributed;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Robbe on 6/12/2016.
 */
public class FileAgent implements AgentInterface {
    /**
     * LockedFilesMap bevat alle beschikbare bestanden en per bestand de hash van een node
     * die er een lock op heeft geplaatst. Indien er geen enkele node een lock heeft op een
     * beschikbaar bestand is de value -1.
     */
    private HashMap<String,Integer> lockedFilesMap = new HashMap<>();
    private Node currentNode;

    /**
     *
     * @throws NullPointerException
     */
    @Override
    public void run() throws NullPointerException {
        if (currentNode == null) throw new NullPointerException("Reference to currentNode is null");
        else {
            /*
            We gaan eerst de lijst van locale files op de huidige node af om te kijken of
            er nieuwe bestanden zijn bijgekomen, zoja voegen we deze toe aan de lockedFilesMap
            en zetten we de waarde op -1 om aan te geven dat er geen enkele node een lock
            heeft op dit bestand
             */
            HashMap<String,FileEntry> currentNodeLocalFiles = currentNode.getLocalFiles();
            for (Map.Entry<String,FileEntry> entry : currentNodeLocalFiles.entrySet()) {
                if (!lockedFilesMap.containsKey(entry.getKey())) {
                    lockedFilesMap.put(entry.getKey(),-1);
                }
            }

            /*
            Vervolgens gaan we de outdated fileList van de huidige node opvragen. Vooraleer we echter
            de fileList van de node updaten gaan we eerst na of de node lock had aangevraagd. Tot slot
            gaan we
             */
            HashMap<String,Boolean> currentNodeFileList = currentNode.getFileList();
            for (Map.Entry<String,Boolean> entry : currentNodeFileList.entrySet()) {
                // Nagaan of er locks werden aangevraagd
                if (lockedFilesMap.containsKey(entry.getKey())){
                    /*
                     Ga na of de request tot lock flag staat en er niemand anders is die
                     al een lock bezit op die file.
                      */
                    if (entry.getValue() && (lockedFilesMap.get(entry.getKey()) != -1)) {

                    }
                    else {

                    }
                }
                // Updaten van de outdated lijst van de huidige node
                else {

                }
            }
        }
    }

    /**
     *
     * @param node de node waarop de agent zich momenteel bevindt.
     */
    @Override
    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }
}
