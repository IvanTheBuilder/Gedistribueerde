package lab.distributed.gui;

import lab.distributed.NameServerBootstrapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Ivan on 22/11/2016.
 */
public class Startup extends JFrame{
    private JPanel panel1;
    private JButton startNodeButton;
    private JButton startNameserverButton;

    public Startup() {
        setContentPane(panel1);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        startNameserverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new WaitingForRMI().setVisible(true);
                NameServerBootstrapper.startRMIRegistry();
            }
        });
        panel1.setVisible(true);
    }
}
