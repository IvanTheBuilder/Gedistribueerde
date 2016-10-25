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
    private String nameServerName = "";

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

    public void getFileLocation(String fileName)
    {
        try {
            NameServerInterface nameServerInterface = (NameServerInterface) Naming.lookup(nameServerName);
            String fileLocation = nameServerInterface.getOwner(fileName);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

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