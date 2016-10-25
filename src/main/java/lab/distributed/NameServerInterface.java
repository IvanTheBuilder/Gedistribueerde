package lab.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Ivan on 25/10/2016.
 */
public interface NameServerInterface extends Remote {

    String getOwner(String filename) throws RemoteException;

    boolean addNode(String nodeName, String inetAddress) throws RemoteException;

    boolean removeNode(String nodeName) throws RemoteException;

}
