package lab.distributed;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ivan on 12/10/2016.
 */
@XmlRootElement(name = "nameserver")
public class NameServer {

    @XmlElement(name = "nodemap")
    Map<Integer, String> nodeMap = new HashMap<Integer, String>();

    public static NameServer fromDisk() {
        try {
            JAXBContext context = JAXBContext.newInstance(NameServer.class);

            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (NameServer) unmarshaller.unmarshal(new File("nameserver.xml"));

        } catch (JAXBException e) {
            e.printStackTrace();
            System.out.println("File probably not found...");
            return null;
        }
    }

    public static final int hashName(String name) {
        return Math.abs(name.hashCode() >> 16);
    }

    /**
     * Voeg een node toe aan het systeem.
     *
     * @param nodeName    String, de naam van deze node
     * @param inetAddress String, het IP-adres van deze node.
     * @return Indien de node is toegevoegd, return true. Indien reeds een node met zelfde naaam, return false
     */
    public boolean addNode(String nodeName, String inetAddress) {
        if (!nodeMap.containsKey(hashName(nodeName))) {
            nodeMap.put(hashName(nodeName), inetAddress);
            return true;
        } else
            return false;
    }

    /**
     * Verwijder een node uit het systeem
     *
     * @param nodeName String, de naam van de node
     * @return Geeft true terug als node is gevonden en verwijdert. Geeft false indien node niet gevonden.
     */
    public boolean removeNode(String nodeName) {
        return nodeMap.remove(hashName(nodeName)) != null;
    }

    /**
     * Vraagt de naam van de node op die "ownership" heeft over een bestand.
     *
     * @param filename Het pad van het bestand.
     * @return
     */
    public String getOwner(String filename) {
        int fileHash = hashName(filename);
        Integer[] keyArray = new Integer[nodeMap.size()];
        nodeMap.keySet().toArray(keyArray);
        Arrays.sort(keyArray);
        int closest = -1;
        for (Integer integer : keyArray) {
            if (integer > closest && integer < fileHash) {
                closest = integer;
            } else
                return nodeMap.get(closest == -1 ? keyArray[keyArray.length] : closest);
        }
        if (closest != -1)
            return nodeMap.get(closest);
        else return null;
    }

    public void saveToDisk() {
        try {
            JAXBContext context = JAXBContext.newInstance(NameServer.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, new File("nameserver.xml"));

            Unmarshaller unmarshaller = context.createUnmarshaller();
            NameServer xmlunmarshalled = (NameServer) unmarshaller.unmarshal(new File("nameserver.xml"));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }


}
