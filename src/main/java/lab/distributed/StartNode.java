package lab.distributed;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Ivan on 7/12/2016.
 */
public class StartNode {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Enter node name: ");
        String name = scanner.nextLine();
        System.out.println("Do you want me to start rmiregistry.exe? (y/n)");
        if(scanner.nextLine().equalsIgnoreCase("y")) {
            Process process = NameServerBootstrapper.startRMIRegistry();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("Stopping rmiregistry.exe ...");
                    process.destroy();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        System.out.println("Trying to start node with name: "+name);
        Node node = new Node(name);
        while(true) {
            String nextline = scanner.nextLine();
            String[] command = nextline.split(" ");
            switch (command[0].toLowerCase()) {
                case "exit":
                    node.exit();
                    break;
                case "status":
                    if(node.getNameServer() != null) {
                        System.out.println("Nameserver has been found");
                    } else {
                        System.out.println("Nameserver has not been found yet.");
                    }
                    System.out.println("Previous node: "+node.getPreviousNode());
                    System.out.println("Next node: "+node.getNextNode());
                    break;
                case "fail":
                    System.out.println("Leaving the network without telling anyone...");
                    System.exit(1);
                    break;
                case "verifyrmi":
                    break;
                case "listnodes":
                    NameServerInterface nameServerInterface = node.getNameServer();
                    //nameServerInterface.getNodeMap().entrySet().forEach(entry -> System.out.printf(""));
                    break;
                case "openfile":
                    if(command.length < 2) {
                        System.out.println("Usage: openfile <filename>");
                    } else {
                        String filename = nextline.substring(9);
                        System.out.println("Trying to request " + filename);
                        File file = node.displayFile(filename);
                        try {
                            if(file != null)
                            Desktop.getDesktop().open(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "listfiles":
                    System.out.println("Local files: ");
                    node.getLocalFiles().entrySet().forEach(entry -> System.out.printf("%s: %s\n", entry.getKey(), entry.getValue()));
                    System.out.println("Replicated files: ");
                    node.getReplicatedFiles().entrySet().forEach(entry -> System.out.printf("%s: %s\n", entry.getKey(), entry.getValue()));
                    System.out.println("File List van Agent:");
                    node.getFileList().entrySet().forEach(entry -> System.out.printf("%s: %s\n", entry.getKey(), entry.getValue()));
                    break;
                default:
                    System.out.println("Unknown command: "+command[0]);

            }
        }

    }

}
