package lab.distributed;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fileserver die wordt gebruikt om bestanden over tcp te sturen
 * Created by Ivan on 10/11/2016.
 */
public class FileServer {

    ServerSocket serverSocket;

    DataInputStream in;
    DataOutputStream out;
    private static final Path LOCAL_DIRECTORY = Paths.get("local");
    private static final Path REPLICATED_DIRECTORY = Paths.get("replicated");

    public FileServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to start fileserver, aborting...");
            System.exit(1);
        }
        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while(true) {
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("[Fileserver] Ready for new connection.");
                            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                            String command = in.readUTF();
                            String fileName = in.readUTF();
                            if(command.equalsIgnoreCase("send")) {
                                File file = getFile(fileName);
                                if(file.exists()) {
                                    System.out.printf("[Fileserver] File %s found. Starting filetransfer...\n", fileName);
                                    InputStream fileStream = new FileInputStream(file);
                                    int count;
                                    byte[] bytes = new byte[8192];
                                    while ((count = fileStream.read(bytes)) > 0) {
                                        out.write(bytes, 0, count);
                                    }
                                    out.flush();
                                    clientSocket.close();
                                } else {
                                    // Indien we het bestand niet hebben, breek de verbinding af.
                                    System.out.printf("[Fileserver] File %s not found. Closing connection...\n", fileName);
                                    in.close();
                                    out.close();
                                    clientSocket.close();
                                }
                            } else if(command.equalsIgnoreCase("receive")) {
                                FileOutputStream fileOutputStream = new FileOutputStream("./replicated/"+fileName);
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
            }).start();
    }

    public File getFile(String name) {
        return new File(REPLICATED_DIRECTORY + File.separator + name);
    }

}
