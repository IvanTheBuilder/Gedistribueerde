package lab.distributed;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Ivan on 10/11/2016.
 */
public class FileServer {

    ServerSocket serverSocket;

    DataInputStream in;
    DataOutputStream out;

    public FileServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Filesocket opened.");
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Fileserver ready for new connection.");
                DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                String command = in.readUTF();
                String fileName = in.readUTF();
                if(command.equalsIgnoreCase("send")) {
                File file = getFile(fileName);
                if(file.exists()) {
                    System.out.printf("File %s found. Starting filetransfer...\n", fileName);
                    InputStream fileStream = new FileInputStream(file);
                    int count;
                    byte[] bytes = new byte[8192];
                    while ((count = fileStream.read(bytes)) > 0) {
                        out.write(bytes, 0, count);
                    }
                    clientSocket.close();
                } else {
                    // Indien we het bestand niet hebben, breek de verbinding af.
                    System.out.printf("File %s not found. Closing connection...\n", fileName);
                    in.close();
                    out.close();
                    clientSocket.close();
                }
                } else if(command.equalsIgnoreCase("receive")) {
                    FileOutputStream fileOutputStream = new FileOutputStream("./files/"+fileName);
                    byte[] bytes = new byte[8192];
                    int count;
                    while ((count = in.read(bytes)) > 0) {
                        fileOutputStream.write(bytes, 0, count);
                    }
                    out.close();
                    in.close();
                    clientSocket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getFile(String name) {
        return new File("./files/"+name);
    }

}
