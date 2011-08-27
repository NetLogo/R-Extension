package org.nlogo.extension.r.systemcheck;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class XFrame  
implements ActionListener { //extends JFrame{

	javax.swing.JPanel panel = new JPanel();
	//private final JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private JFrame frame = new JFrame("Message");
;
	
	public static void main(String[] args)
	{
		XFrame f = new XFrame("Hallo Test!\nBlaBla.\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile" +
				"\nund noch eine Zeile\nund noch eine Zeile\nund noch eine Zeile", "Title");
	}
	
	public XFrame(String message, String title) {
		frame.setTitle(title);
		JPanel consolePanel = new JPanel();
		consolePanel.setLayout(new BorderLayout());

		JTextArea textarea = new JTextArea();
		textarea.setText(message);
		textarea.setEditable(false);

		JScrollPane pane = new JScrollPane(textarea);
		consolePanel.add(pane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		FlowLayout jPanel1Layout = new FlowLayout();
		consolePanel.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(jPanel1Layout);
		
		JButton closeButton = new JButton();
		buttonPanel.add(closeButton);
		closeButton.setText("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		closeButton.setPreferredSize(new java.awt.Dimension(98, 21));

		frame.getContentPane().add(consolePanel);

		frame.setSize(400, 500);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		frame.dispose();
		
	}
}
