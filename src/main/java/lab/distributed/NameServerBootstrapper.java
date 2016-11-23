package lab.distributed;

import lab.distributed.gui.NameserverGUI;

import java.io.File;
import java.io.IOException;
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
        System.out.println("Starting RMI-server...");
        startRMIRegistry();
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

    public static void startNameserver(NameserverGUI nameserverGUI) {

    }

    public static void startRMIRegistry() {
        String javaHome = System.getProperty("java.home");
        ProcessBuilder processBuilder = new ProcessBuilder(javaHome+ File.separator+"bin"+File.separator+"rmiregistry.exe");
        processBuilder.directory(new File("./target/classes"));
        try {
            Process process = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    process.destroyForcibly();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
