package lab.distributed;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Robrecht on 11/8/2016.
 */
public class TestNode1 {
    public static void main(String[] args) {
        //NameServerBootstrapper.startRMIRegistry();
        Node node = new Node("Robbe");
        //elke keer dat de next en previous node wordt geupdated wordt dit naar de terminal geprint
        //om failure te testen: na een tijdje een netwerkkabel uittrekken en kijken of de andere nodes geupdated worden.
        //node.sendPing();
        System.out.println("geef de naam van een bestand om te openen");
        Scanner scanner = new Scanner(System.in);
        //
        //  node.sendFile(30888, scanner.nextLine());
        String naam = scanner.nextLine();// wachten op invoer van de gebruiker
        File file = node.displayFile(naam);
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("geef enter in om af te sluiten");
        scanner.nextLine();
        /*try {
            System.out.println("sleeping....");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        node.exit();//afsluiten testen
        System.out.println("node afgesloten");
    }

}
