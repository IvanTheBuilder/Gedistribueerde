package lab.distributed;

/**
 * Created by licensed on 11/8/2016.
 */
public class TestNode1 {
    public static void main(String[] args) {
        Node node = new Node("Node1");
        //elke keer dat de next en previous node wordt geupdated wordt dit naar de terminal geprint
        //om failure te testen: na een tijdje een netwerkkabel uittrekken en kijken of de andere nodes geupdated worden.
        System.out.println("node is opgestart");
        //Scanner scanner = new Scanner(System.in);
        //scanner.nextLine();// wachten op invoer van de gebruiker
        /*try {
            System.out.println("sleeping....");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        node.exit();//afsluiten testen
        System.out.println("node afgesloten");*/
    }

}
