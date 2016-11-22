package lab.distributed.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by Ivan on 22/11/2016.
 */
public class NameserverGUI extends JFrame {
    private JList nodeList;
    private JPanel panel1;
    private JTextPane logPane;

    public NameserverGUI() {
        setContentPane(panel1);
        pack();
    }

    public void updateNodeMap(TreeMap<Integer, String> nodelijst) {
        ArrayList<String> listData = new ArrayList<>();
        nodelijst.entrySet().forEach(e -> listData.add(e.getKey()+": "+e.getValue()));
        nodeList.setListData(listData.toArray(new String[listData.size()]));
    }

}
