package lab.distributed;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Ivan on 24/10/2016.
 */
public class Node {

    private String name;
    private String location;
    private String nameServerName = "//192.168.1.1/NameServerInterface";

    /**
     * De constructor gaat een nieuwe node aanmaken in de nameserver met de gekozen naam en het ip adres van de machine waarop hij gestart wordt.
     * @param name de naam van de node
     */
    public Node(String name){
        this.name=name;
        try {
            location=InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if(!nameServerInterface.addNode(name,location))
            {
                System.out.println("deze naam bestaat al");
                System.exit(1);
            }
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Deze node kan eender welke node verwijderen uit de nameServer
     * @param name de naam van te verwijderen node
     */
    public void deleteNode(String name)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            if(!nameServerInterface.removeNode(name))
                System.out.println("deze node bestaat niet");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Deze methode zal de locatie van een bestand opzoeken in de nameserver
     * @param fileName de naam van het te zoeken bestand
     * @return het ip adres van de locatie van het bestand
     */
    public String getFileLocation(String fileName)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            String fileLocation = nameServerInterface.getOwner(fileName);
            return fileLocation;
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  deze node wordt verwijderd uit de nameserver en sluit af
     */
    public void exit()
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            deleteNode(name);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }finally {
            System.exit(0);
        }

    }

}