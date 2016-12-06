package lab.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface van de nameserver waardoor we de nameserver via RMI kunnen aanroepen
 * Created by Ivan on 25/10/2016.
 */
public interface NameServerInterface extends Remote {

    /**
     * Vraagt de naam van de node op die "ownership" heeft over een bestand.
     *
     * @param filename Het pad van het bestand.
     * @return het ip adres van van de owner
     */
    String getOwner(String filename) throws RemoteException;

    /**
     * Voeg een node toe aan het systeem.
     *
     * @param nodeName    String, de naam van deze node
     * @param inetAddress String, het IP-adres van deze node.
     * @return Indien de node is toegevoegd, return true. Indien reeds een node met zelfde naaam, return false
     */
    boolean addNode(String nodeName, String inetAddress) throws RemoteException;

    /**
     * Verwijder een node uit het systeem
     *
     * @param nodeName String, de hash(id) van de node
     * @return Geeft true terug als node is gevonden en verwijderd. Geeft false indien node niet gevonden.
     */
    boolean removeNode(int nodeName) throws RemoteException;

    int getOwnerHash(String filename);

    /**
     * Vraag het IP op van een node.
     *
     * @param hash de hash van de gevraagde node
     * @return het ip adtes van de gevraagde node
     */
    String getAddress(int hash) throws RemoteException;

    /**
     * geeft de volgende node van de gevraagde node terug
     * @param hash hash van de gevraagde node
     * @return hash van de volgende node
     */
    int getNextNode(int hash) throws RemoteException;

    /**
     * geeft de vorige node van de gevraagde node terug
     * @param hash hash van de gevraagde node
     * @return hash van de vorige node
     */
    int getPreviousNode(int hash) throws RemoteException;

}
