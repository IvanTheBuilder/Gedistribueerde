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
    }

    public void getLocation(String fileName)
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

}