package lab.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

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

}
