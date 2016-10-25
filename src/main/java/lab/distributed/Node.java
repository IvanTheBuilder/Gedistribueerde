package lab.distributed;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Ivan on 24/10/2016.
 */
public class Node {

    private String name;
    private String location;

    public Node(String name){
        this.name=name;
        try {
            location=InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}