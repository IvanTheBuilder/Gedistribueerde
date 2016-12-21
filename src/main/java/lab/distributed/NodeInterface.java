package lab.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Interface van de Node waardoor we een node via RMI kunnen bereiken
 * Created by Ivan on 15/11/2016.
 */
public interface NodeInterface extends Remote {

    /**
     * verander de vorige node van deze node
     * @param hash de hash van de nieuwe vorige node
     * @throws RemoteException
     */
    void setPreviousNode(int hash) throws RemoteException;

    /**
     * verander de volgende node van deze node
     * @param hash de hash van de nieuwe volgende node
     * @throws RemoteException
     */
    void setNextNode(int hash) throws RemoteException;

    /**
     * methode die kan aangeroepen worden om een boodschap naar de terminal te printen vanuit een andere node, gebruikt om te testen
     * @param message de boodschap die moet afgedrukt worden
     * @throws RemoteException
     */
    void printMessage(String message) throws RemoteException;

    /**
     * roep deze methode op om een bestand te repliceren naar deze node
     * @param entry: de bestandsfiche van het te repliceren bestand
     */
    void replicateNewFile(FileEntry entry) throws RemoteException;

    /**
     * De nameserver gaat deze methode aanroepen bij het opstarten om de grootte van het netwerk te kennen te geven
     * @param ip ip van de nameserver
     * @param size grootte van het netwerk
     * @throws RemoteException
     */
    void setSize(String ip, int size) throws RemoteException;

    /**
     * past een bestandsfiche aan van de lokale bestanden
     * @param name naam van het bestand
     * @param entry de nieuwe bestandsfiche
     * @return true als het gelukt is
     * @throws RemoteException
     */
    boolean changeLocalEntry(String name, FileEntry entry) throws RemoteException;

    /**
     * past een bestandsfiche aan van de replicated bestanden
     * @param name naam van het bestand
     * @param entry de nieuwe bestandsfiche
     * @return true als het gelukt is
     * @throws RemoteException
     */
    boolean changeReplicatedEntry(String name, FileEntry entry) throws RemoteException;

    /**
     * Verwijdert het bestand van de node
     * @param naam naam van het te verwijderen bestand
     * @return true als het bestand bestaat en verwijdert is
     * @throws RemoteException
     */
    boolean deleteReplicatedFile(String naam) throws RemoteException;

    /**
     * Deze methode zal de agent op deze node opstarten en de agent op de volgende node starten wanneer hij hier klaar is.
     * @param agent de agent die gestart moet worden
     * @throws RemoteException
     */
    void startAgent(AgentInterface agent) throws RemoteException;

    /**
     * Haalt een fileEntry op vanop een adnere node, via RMI oproepen
     * @param fileName fileName van de entry die je wil hebben
     * @return de fileEntry die bij fileName hoort
     * @throws RemoteException
     */
    FileEntry getRemoteFileEntry(String fileName) throws RemoteException;

    /**
     * gets local files hashmap
     * @return local files hashmap
     * @throws RemoteException
     */
    HashMap<String, FileEntry> getLocalFiles() throws  RemoteException;

    /**
     * gets replicated files hashmap
     * @return replicated files hashmap
     * @throws RemoteException
     */
    HashMap<String, FileEntry> getReplicatedFiles() throws RemoteException;


    /**
     * geeft de hash van deze node terug
     * @return de hash van deze node
     * @throws RemoteException
     */
    int getMyHash() throws RemoteException;
}
