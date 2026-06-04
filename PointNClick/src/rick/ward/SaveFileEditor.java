package rick.ward;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SaveFileEditor {

	public static void main(String[] args) {
		JFrame frame = new EditorWindow();
		
	}
}

class EditorWindow extends JFrame {
	JLabel version;
	JLabel fileNameLabel;
	JTextField fileName;
	JTextField Splash;
	JButton LoadButton;
	JButton SaveButton;
	JButton NukeButton;
	JPanel NorthPanel = new JPanel();
	JPanel CentrePanel = new JPanel();
	public EditorWindow() {
		NorthPanel.setLayout(new FlowLayout());
		CentrePanel.setLayout(new FlowLayout());
		setTitle("PointNClick Game Save File Editor");
		setSize(GameWindow.WIDTH, GameWindow.HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        version = new JLabel("Global Game Version: " + GameWindow.GameVersion);
        NorthPanel.add(version);
        Splash = new JTextField("");
        Splash.setEditable(false);
        NorthPanel.add(Splash);
        LoadButton = new JButton("Load");
        CentrePanel. add(LoadButton);

        SaveButton = new JButton("Save");
        CentrePanel.add(SaveButton);
        
        NukeButton = new JButton("Nuke");
        CentrePanel.add(NukeButton);
        
        add(CentrePanel, BorderLayout.CENTER);
        add(NorthPanel, BorderLayout.NORTH);
        pack();
        setVisible(true);
        setResizable(false);
	}
	public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals("Nuke")) {

        	if(Splash.getText().equals("BOOM!")) {
        		Splash.setText("");
        	} else {
        		Splash.setText("BOOM!");
        	}
        }
    }
}
