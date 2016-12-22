package lab.distributed.gui;

import lab.distributed.Node;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Created by Ivan on 20/12/2016.
 */
public class FileGUI extends JFrame {
    private JPanel panel1;
    private JList list1;
    private JButton openButton;
    private JButton deleteLocalButton;
    private JButton deleteButton;
    private JLabel namelabel;
    private JLabel ownerlabel;
    private JLabel replicatedlabel;
    private Node node;
    private String[] lastArray = new String[]{};

    public FileGUI(Node node) {
        this.setTitle("Gedistribueerde Systemen LOLOLOLOLO");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        setContentPane(panel1);
        openButton.setEnabled(false);
        deleteLocalButton.setEnabled(false);
        deleteButton.setEnabled(false);
        this.node = node;
        pack();
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                File file = node.displayFile((String) list1.getSelectedValue());
                try {
                    if(file != null)
                        Desktop.getDesktop().open(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        deleteLocalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    node.deleteReplicatedFile((String) list1.getSelectedValue());
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                //TODO: delete deze file lokaal
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Geklikt op deleteKnop! File entry is "+list1.getSelectedValue());
                //TODO: delete dit bestand in het netwerk
            }
        });

        list1.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                    if(list1.getSelectedValue() != null) {
                        openButton.setEnabled(true);
                        deleteButton.setEnabled(true);
                        //TODO: check if dit bestand lokaal staat. Zo niet disable deze knop, zo wel enable deze knop
                        deleteLocalButton.setEnabled(false);
                    } else {
                        openButton.setEnabled(false);
                        deleteLocalButton.setEnabled(false);
                        deleteButton.setEnabled(false);
                    }
                    System.out.println("Nieuwe file-entry geselecteerd! "+list1.getSelectedValue());
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(node != null && node.getFileList() != null) {
                        if(lastArray.length != node.getFileList().size()) {
                            lastArray = node.getFileList().keySet().toArray(new String[node.getFileList().size()]);
                            list1.setListData(lastArray);
                        }
                    }
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        FileGUI fileGUI = new FileGUI(null);
        fileGUI.setVisible(true);
    }

    //TODO: roep deze methode aan telkens wanneer er iets veranderd in de filelist
    public void refreshFileList(String[] fileEntries) {
        list1.setListData(fileEntries);

    }




}
