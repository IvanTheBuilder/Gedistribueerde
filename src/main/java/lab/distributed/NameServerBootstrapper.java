package lab.distributed;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Ivan on 25/10/2016.
 */
public class NameServerBootstrapper {

    public static void main(String[] args) {
        System.out.println("Starting NameServer...");
        NameServer nameServer = new NameServer();
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) UnicastRemoteObject.exportObject(nameServer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("NameServerInterface", nameServerInterface);
            System.out.println("RMI-Server ready. NameServer initialized");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }


}
