package com.mch.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.mch.controller.Client;

public class ClientGUI extends JFrame implements ActionListener,
		ClientGUInterface {

	private static final long serialVersionUID = 1L;
	private static String defTitle = "Chat Client";
	// will first hold the "user name", later the "message"
	private JLabel label;
	// to hold the user name and later on the messages
	private JTextField txtUserMessage;
	// to hold the server address an the port number
	private JTextField tfServer, tfPort;
	// to Logout and get the list of the users
	private JButton btnLogin, btnLogout, btnWhoIsIn, btnSend;
	// display messages here
	private JTextArea areaMessages;
	// Combo box with users
	private JComboBox<String> cmbUsers;
	// if it is for connection
	private boolean connected;
	// the Client object
	private Client client;
	private String username;
	// the default port number
	private int serverPort;
	private String serverHost;

	// Constructor connection receiving a socket number
	public ClientGUI(String host, int port) {
		super(defTitle);
		this.serverPort = port;
		this.serverHost = host;

		addGUIElements();
	}

	private void addGUIElements() {
		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(4, 1));
		// the server name and the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		JPanel panelUserSend = new JPanel();
		// the two JTextField with default value for server address and port
		// number
		tfServer = new JTextField(serverHost);
		tfPort = new JTextField("" + serverPort);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		serverAndPort.add(new JLabel(""));
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);

		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		txtUserMessage = new JTextField("Anonymous");
		txtUserMessage.setBackground(Color.WHITE);
		northPanel.add(txtUserMessage);
		cmbUsers = new JComboBox<String>();
		panelUserSend.add(cmbUsers);
		btnWhoIsIn = new JButton("Refresh");
		panelUserSend.add(btnWhoIsIn);
		btnSend = new JButton("Send");
		panelUserSend.add(btnSend);
		northPanel.add(panelUserSend);
		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		areaMessages = new JTextArea("Welcome to the Chat room\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		centerPanel.add(new JScrollPane(areaMessages));
		areaMessages.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);

		// the 3 buttons
		btnLogin = new JButton("Login");
		btnLogin.addActionListener(this);
		btnLogout = new JButton("Logout");
		btnLogout.addActionListener(this);
		btnLogout.setEnabled(false); // you have to login before being able to
										// logout
		btnWhoIsIn.addActionListener(this);
		btnWhoIsIn.setEnabled(false); // you have to login before being able to
										// Who is in
		btnSend.addActionListener(this);
		btnSend.setEnabled(false); // you have to login before being able to Who
									// is in

		JPanel southPanel = new JPanel();
		southPanel.add(btnLogin);
		southPanel.add(btnLogout);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		txtUserMessage.requestFocus();
	}

	// Button or JTextField clicked
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		// if it is the Logout button
		if (o == btnLogout) {
			client.doLogout();
			this.setTitle(defTitle);
			return;
		}
		// if it the who is in button
		if (o == btnWhoIsIn) {
			client.doGetUserList();
			return;
		}
		// OK it is coming from the JTextField
		if (o == btnSend || connected) {
			// just have to send the message
			String to = ((String) cmbUsers.getSelectedItem()).split("--")[0]
					.trim();
			client.doSendMessage(txtUserMessage.getText(), to);
			txtUserMessage.setText("");
			return;
		}

		if (o == btnLogin) {
			username = txtUserMessage.getText().trim();
			if (username.isEmpty())
				return;
			// empty serverAddress ignore it
			String server = tfServer.getText().trim();
			if (server.isEmpty())
				return;
			// empty or invalid port number, ignore it
			String portNumber = tfPort.getText().trim();
			if (portNumber.isEmpty())
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (NumberFormatException en) {
				return;
			}

			// try creating a new Client with GUI
			client = new Client(server, port, username, this);
			// test if we can start the Client
			if (!client.start())
				return;
			txtUserMessage.setText("");
			this.setTitle(defTitle + ": " + username);
			label.setText("Enter your message below");
			connected = true;

			// disable login button
			btnLogin.setEnabled(false);
			// enable the 2 buttons
			btnLogout.setEnabled(true);
			btnWhoIsIn.setEnabled(true);
			btnSend.setEnabled(true);
			// disable the Server and Port JTextField
			tfServer.setEditable(false);
			tfPort.setEditable(false);
			// Action listener for when the user enter a message
			txtUserMessage.addActionListener(this);
			client.doGetUserList();
		}

	}

	// called by the Client to append text in the TextArea
	@Override
	public void append(String str) {
		str += "\n";
		areaMessages.append(str);
		areaMessages.setCaretPosition(areaMessages.getText().length() - 1);
	}

	// called by the GUI is the connection failed
	// we reset our buttons, label, text field
	@Override
	public void connectionFailed() {
		btnLogin.setEnabled(true);
		btnLogout.setEnabled(false);
		btnWhoIsIn.setEnabled(false);
		label.setText("Enter your username below");
		txtUserMessage.setText("Anonymous");
		// reset port number and host name as a construction time
		tfPort.setText("" + serverPort);
		tfServer.setText(serverHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		// don't react to a <CR> after the user name
		txtUserMessage.removeActionListener(this);
		connected = false;
	}

	@Override
	public void setUsers(List<String> users) {
		cmbUsers.setModel(new DefaultComboBoxModel<String>());
		for (String u : users)
			cmbUsers.addItem(u);
	}
}
