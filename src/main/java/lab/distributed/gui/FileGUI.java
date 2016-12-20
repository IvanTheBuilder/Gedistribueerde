package lab.distributed.gui;

import lab.distributed.FileEntry;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    public FileGUI() {
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
        pack();
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Geklikt op open knop! File entry is "+list1.getSelectedValue());
                //TODO: open deze fileentry
            }
        });

        deleteLocalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Geklikt op deleteLocal knop! File entry is "+list1.getSelectedValue());
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
                        FileEntry fileEntry = (FileEntry) list1.getSelectedValue();
                        openButton.setEnabled(true);
                        deleteButton.setEnabled(true);
                        //TODO: check if dit bestand lokaal staat. Zo niet disable deze knop, zo wel enable deze knop
                        deleteLocalButton.setEnabled(false);
                        namelabel.setText(fileEntry.getFileName());
                        ownerlabel.setText(fileEntry.getOwner());
                        replicatedlabel.setText(fileEntry.getReplicated());
                    } else {
                        openButton.setEnabled(false);
                        deleteLocalButton.setEnabled(false);
                        deleteButton.setEnabled(false);
                    }
                    System.out.println("Nieuwe file-entry geselecteerd! "+list1.getSelectedValue());
                }
            }
        });
    }

    public static void main(String[] args) {
        FileGUI fileGUI = new FileGUI();
        fileGUI.setVisible(true);
        fileGUI.refreshFileList(new FileEntry[]{new FileEntry("Testnaam", "lokaal", "Owner", "Replicated"), new FileEntry("Testnaam2", "lokaal", "Owner", "Replicated")});
    }

    //TODO: roep deze methode aan telkens wanneer er iets veranderd in de filelist
    public void refreshFileList(FileEntry[] fileEntries) {
        list1.setListData(fileEntries);

    }


}
