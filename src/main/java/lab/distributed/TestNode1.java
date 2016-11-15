package lab.distributed;

import java.rmi.RemoteException;
import java.util.Scanner;

/**
 * Created by licensed on 11/8/2016.
 */
public class TestNode1 {
    public static void main(String[] args) {
        Node node = new Node("Ivannnnnn");
        //elke keer dat de next en previous node wordt geupdated wordt dit naar de terminal geprint
        //om failure te testen: na een tijdje een netwerkkabel uittrekken en kijken of de andere nodes geupdated worden.
        node.sendPing();
        Scanner scanner = new Scanner(System.in);
     //
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            node.getNode(1289).printMessage("Test!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //  node.sendFile(30888, scanner.nextLine());
        scanner.nextLine();// wachten op invoer van de gebruiker
        /*try {
            System.out.println("sleeping....");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        node.exit();//afsluiten testen
        System.out.println("node afgesloten");
    }

}
