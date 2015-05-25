package com.mch.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import com.mch.exception.ChatMessageException;
import com.mch.model.ChatMessage;
import com.mch.view.ClientGUInterface;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {

	// for I/O
	private ObjectInputStream sInput; // to read from the socket
	private ObjectOutputStream sOutput; // to write on the socket
	private Socket socket;

	// if I use a GUI or not
	private ClientGUInterface cg;

	// the server, the port and the user name
	private String server, username;
	private int port;

	public Client(String server, int port, String username, ClientGUInterface cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.cg = cg;
	}

	// start the dialog
	@SuppressWarnings("unchecked")
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		}
		// if it failed not much I can so
		catch (Exception ec) {
			updateView("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":"
				+ socket.getPort();
		updateView(msg);

		/* Creating both Data Stream */
		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException eIO) {
			updateView("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// Send user name to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try {
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			updateView("Exception doing login : " + eIO);
			disconnect();
			return false;
		}

		// wait the server response if the user name is not taken, block the
		// client GUI
		ChatMessage<Boolean> cm = null;
		try {
			cm = (ChatMessage<Boolean>) sInput.readObject();
		} catch (ClassNotFoundException e) {
			updateView(e.getMessage());
		} catch (IOException e) {
			updateView(e.getMessage());
		}
		if (cm.getType() != ChatMessage.LOGIN || cm.getMessage() != true) {
			updateView("User name taken.");
			return false;
		}

		// creates the Thread to listen from the server
		new ListenFromServer().start();

		// success we inform the caller that it worked
		return true;
	}

	public boolean doSendMessage(String msgText, String to) {
		ChatMessage<?> msg = new ChatMessage<String>(ChatMessage.MESSAGE,
				msgText, username, to);
		return sendMessage(msg);
	}

	public void doGetUserList() {
		sendMessage(new ChatMessage<String>(ChatMessage.WHOISIN, "", null, null));
	}

	public void doLogout() {
		sendMessage(new ChatMessage<String>(ChatMessage.LOGOUT, "", null, null));
	}

	// solve this with Observer? ..
	private void updateView(String msg) {
		cg.append(msg);
	}

	// send a message to the server
	private boolean sendMessage(ChatMessage<?> msg) {
		try {
			sOutput.writeObject(msg);
			return true;
		} catch (IOException e) {
			updateView("Exception writing to server: " + e);
		}
		return false;
	}

	// if something goes wrong Close the Input/Output streams and disconnect
	private void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
			if (sOutput != null)
				sOutput.close();
			if (socket != null)
				socket.close();
		} catch (Exception e) {
			updateView(e.getMessage());
		}
		// inform the GUI
		if (cg != null)
			cg.connectionFailed();
	}

	// a class that waits for the message from the server
	private class ListenFromServer extends Thread {

		@SuppressWarnings("unchecked")
		public void run() {
			while (true) {
				try {
					// all results are received here
					Object o = sInput.readObject();
					if (o instanceof ChatMessage<?>) {
						ChatMessage<?> msg = (ChatMessage<?>) o;

						if (msg.getType() == ChatMessage.WHOISIN) {
							// message type should be List<String>
							cg.setUsers((List<String>) msg.getMessage()); // ..
						} else
							cg.append(msg.getFrom() + ": " + msg.getMessage());
					} else
						throw new ChatMessageException(
								"Did not receive ChatMessage object from stream.");
				} catch (IOException e) {
					updateView("Server has close the connection: " + e);
					if (cg != null)
						cg.connectionFailed(); // ..
					// break the while loop, finish listening
					break;
				} catch (ChatMessageException e2) {
					updateView(e2.getMessage());
				} catch (ClassNotFoundException e3) {
					updateView("Error reading from stream in client: "
							+ e3.getMessage());
				}
			}
		}
	}
}
