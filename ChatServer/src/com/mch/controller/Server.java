package com.mch.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mch.exception.ChatMessageException;
import com.mch.model.ChatMessage;
import com.mch.view.ServerGUInterface;

// the actual server
public class Server {
	// list of client handling threads
	// it does not have to be synchronized, all accessing methods are..
	private Map<String, ClientHandlerThread> clientThreads;
	private ServerSocket serverSocket;
	private ServerGUInterface sg;
	private SimpleDateFormat simpleDateFormater;
	// the port number to listen for connection
	private int port;

	public Server(int port, ServerGUInterface sg) {
		// GUI or not
		this.sg = sg;
		// the port
		this.port = port;
		// to display hh:mm:ss
		simpleDateFormater = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		clientThreads = new HashMap<String, ClientHandlerThread>();
	}

	public void start() {
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			serverSocket = new ServerSocket(port);
			System.out.println("Server started.");
			String waitMsg = "Server waiting for Clients on port " + port
					+ "...";
			// infinite loop to wait for connections
			displayEvent(waitMsg);
			// accept connection
			Socket socket = null;
			while ((socket = serverSocket.accept()) != null) {
				// format message saying we are waiting
				displayEvent(waitMsg);

				// make a handler thread
				ClientHandlerThread t = null;
				try {
					t = new ClientHandlerThread(socket);
					// if returned with exception thread wont be created
					clientThreads.put(t.username, t); // save it in the Map
					t.start();
				} catch (ChatMessageException e) {
					displayEvent(e.getMessage());
				}
				// continue with next iteration
			}
			// Error occurred try and stop
			try {
				stop();
			} catch (Exception e) {
				displayEvent("Exception closing the server and clients: " + e);
			}
		} catch (IOException e) {
			// the serverSocket.accept() will return with an Exception,
			// but no problems here
			displayEvent("Server terminated: " + e.getMessage());
		}
	}

	// stop the server
	public void stop() {
		// close client connection
		for (ClientHandlerThread clientThread : clientThreads.values()) {
			clientThread.closeAndStop();
		}
		// close the server socket
		try {
			serverSocket.close();
		} catch (IOException e) {
			displayEvent("" + e);
		}
	}

	private void displayEvent(String msg) {
		String time = simpleDateFormater.format(new Date());
		sg.appendEvent(time + " " + msg);
	}

	// for a client who log off using the LOGOUT message
	synchronized void remove(ClientHandlerThread clientHandler) {
		clientThreads.remove(clientHandler.username);
	}

	/** One instance of this thread will run for each client */
	private class ClientHandlerThread extends Thread {
		// the socket where to listen/talk
		private Socket socket;
		private ObjectInputStream sInput;
		private ObjectOutputStream sOutput;
		// the user name of the Client
		private String username;
		// the date I connect
		private String date;

		// constructor
		ClientHandlerThread(Socket socket) throws ChatMessageException {
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Input/Output Streams");
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());
				// read the user name String, the only time a string is sent,
				// expect ChatMessage after this
				System.out.println("here");
				username = (String) sInput.readObject();
				
				// check if user name is not taken
				if (clientThreads.get(username) != null) {
					// reply and abort
					sOutput.writeObject(new ChatMessage<Boolean>(
							ChatMessage.LOGIN, false, null, username));
					throw new ChatMessageException("User Name taken.");
				} else {
					// reply positively
					sOutput.writeObject(new ChatMessage<Boolean>(
							ChatMessage.LOGIN, true, null, username));
				}

				displayEvent(username + " just connected.");
			} catch (IOException e) {
				throw new ChatMessageException(
						"Exception creating new Input/output Streams.");
			} catch (ClassNotFoundException e) {
				throw new ChatMessageException(
						"Unexpected object read form stream.");
			}
			date = new Date().toString() + "\n";
		}

		// what will run forever
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			System.out.println("Client Handler thread started.");
			// to loop until LOGOUT
			boolean keepGoing = true;
			while (keepGoing) {
				// read Object
				ChatMessage<?> cm = null;
				try {
					cm = (ChatMessage<?>) sInput.readObject();
				} catch (IOException e) {
					displayEvent(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}

				// Switch on the type of message receive
				switch (cm.getType()) {
				case ChatMessage.MESSAGE:
					if (cm.getTo() != null)
						forwardMessage((ChatMessage<String>) cm, this);
					break;
				case ChatMessage.LOGOUT:
					displayEvent(username
							+ " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					List<String> userList = new ArrayList<String>();
					// scan all the users connected
					for (ClientHandlerThread ct : clientThreads.values())
						userList.add(ct.username + " -- since " + ct.date);
					writeMsg(new ChatMessage<List<String>>(ChatMessage.WHOISIN,
							userList, null, null));
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(this);
			closeAndStop();
		}

		// try to close everything
		private void closeAndStop() {
			// try to close the connection
			try {
				if (sOutput != null)
					sOutput.close();
				if (sInput != null)
					sInput.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				displayEvent("Error closing client handler: " + e);
			} finally {
				try {
					this.interrupt();
				} catch (SecurityException ee) {
					displayEvent("Error stopping the client handler thread: "
							+ ee);
				}
			}
		}

		// Write to the Client output stream
		private boolean writeMsg(ChatMessage<?> msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				closeAndStop();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				displayEvent("Error sending message to " + username);
				displayEvent(e.toString());
			}
			return true;
		}

		// write to specific Client
		private synchronized boolean forwardMessage(ChatMessage<String> cm,
				ClientHandlerThread fromC) {
			// add HH:mm:ss and \n to the message
			String time = simpleDateFormater.format(new Date());
			String messageLf = time + " " + cm.getMessage() + "\n";

			sg.appendMessageLog(messageLf); // append in the room window

			System.out.println(cm.getTo());
			System.out.println(clientThreads);
			ClientHandlerThread toC = clientThreads.get(cm.getTo());
			// if client is not online send message back to sender
			if (toC == null) {
				toC = fromC;
				cm = new ChatMessage<String>(cm.getType(), "Client "
						+ cm.getTo() + " does not exist anymore", cm.getFrom(),
						cm.getFrom());
			}
			Socket socket = toC.socket;
			ObjectOutputStream sOutput = toC.sOutput;
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				closeAndStop();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(cm);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				displayEvent("Error sending message to " + username);
				displayEvent(e.toString());
			}
			return true;
		}
	}
}
