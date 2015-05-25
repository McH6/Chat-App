package com.mch.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.mch.controller.Server;

public class ServerGUI extends JFrame implements ActionListener,
		WindowListener, ServerGUInterface {
	private static final long serialVersionUID = 1L;
	// the stop and start buttons
	private JButton btnStartStop;
	// JTextArea for the chat room and the events
	private JTextArea txtMessageLog, txtEvent;
	// The port number
	private JTextField txtPortNumber;
	// my server
	private Server server;
	private int port;

	// gets the port
	public ServerGUI(int port) {
		super("Chat Server");
		this.server = null;
		this.port = port;

		addGUIElements();

		appendEvent("Events log...");
	}

	private void addGUIElements() {
		// in the NorthPanel the PortNumber the Start and Stop buttons
		JPanel north = new JPanel(new FlowLayout());
		north.add(new JLabel("Port number: "));
		txtPortNumber = new JTextField("  " + port);
		north.add(txtPortNumber);
		// to stop or start the server, we start with "Start"
		btnStartStop = new JButton("Start");
		btnStartStop.addActionListener(this);
		north.add(btnStartStop);
		add(north, BorderLayout.NORTH);

		// the event and chat room
		JPanel center = new JPanel(new GridLayout(2, 1));
		txtMessageLog = new JTextArea(80, 80);
		txtMessageLog.setEditable(false);
		appendMessageLog("Message Log...");
		center.add(new JScrollPane(txtMessageLog));
		txtEvent = new JTextArea(80, 80);
		txtEvent.setEditable(false);

		center.add(new JScrollPane(txtEvent));
		add(center);

		// need to be informed when the user click the close button on the frame
		addWindowListener(this);
		setSize(400, 600);
		setVisible(true);
	}

	// append message to the two JTextArea
	// position at the end
	@Override
	public void appendMessageLog(String str) {
		txtMessageLog.append(str);
		txtMessageLog.setCaretPosition(txtMessageLog.getText().length() - 1);
	}

	@Override
	public void appendEvent(String str) {
		str += "\n";
		txtEvent.append(str);
		txtEvent.setCaretPosition(txtEvent.getText().length() - 1);
	}

	// start or stop where clicked
	@Override
	public void actionPerformed(ActionEvent e) {
		// if running we have to stop
		if (server != null) {
			server.stop();
			server = null;
			txtPortNumber.setEditable(true);
			btnStartStop.setText("Start");
			return;
		}
		// OK start the server
		int port;
		try {
			port = Integer.parseInt(txtPortNumber.getText().trim());
		} catch (Exception er) {
			appendEvent("Invalid port number");
			return;
		}
		// create a new Server
		server = new Server(port, this);
		// and start it as a thread
		new ServerBackgroundWorker().start();
		btnStartStop.setText("Stop");
		txtPortNumber.setEditable(false);
	}

	/*
	 * If the user click the X button to close the application I need to close
	 * the connection with the server to free the port
	 */
	@Override
	public void windowClosing(WindowEvent e) {
		// if my Server exist
		if (server != null) {
			try {
				server.stop(); // ask the server to close the connection
			} catch (Exception eClose) {
			}
			server = null;
		}
		// dispose the frame
		dispose();
		System.exit(0);
	}

	// I can ignore the other WindowListener method
	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}


	// a single thread to run the Server
	// otherwise the GUI would be unresponsive
	private class ServerBackgroundWorker extends Thread {
		public void run() {
			System.out.println("Starting server...");
			server.start(); // should execute until if fails
			// the server exited
			btnStartStop.setText("Start");
			txtPortNumber.setEditable(true);
			server = null;
		}
	}

}
