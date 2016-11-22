package lab.distributed.gui;

import javax.swing.*;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by Ivan on 22/11/2016.
 */
public class WaitingForRMI extends JFrame {
    private JPanel panel1;
    private JLabel textlabel;
    private int tries = 0;

    public WaitingForRMI() {
        setContentPane(panel1);
        pack();
        panel1.setVisible(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!isRunning()) {
                    try {
                        if(tries < 5) {
                            tries++;
                            Thread.sleep(500);
                            run();
                        } else {
                            textlabel.setText("RMI failed to start");
                            Thread.sleep(2000);
                            System.exit(1);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    textlabel.setText("Starting Nameserver...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(1);
                }
            }
        }).start();
    }

    public boolean isRunning() {
        try {
            Registry registry = LocateRegistry.getRegistry();
            registry.list();
        } catch (ConnectException e) {
            return false;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

}
