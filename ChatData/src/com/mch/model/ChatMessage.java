package com.mch.model;

import java.io.Serializable;

// Objects from this class will be passed around on the socket
public class ChatMessage<T> implements Serializable {
	protected static final long serialVersionUID = 1112122200L;
	
	// The message fields (can be null also)
	private int type;
	private T message;
	private String from;
	private String to;

	// the Standard message types
	public static final int WHOISIN = 0;
	public static final int MESSAGE = 1;
	public static final int LOGOUT = 2;
	
	public ChatMessage(int type, T message, String from, String to) {
		this.type = type;
		this.message = message;
		this.from = from;
		this.to = to;
	}
	
	
	public int getType() {
		return type;
	}
	public T getMessage() {
		return message;
	}
	public String getFrom() {
		return from;
	}
	public String getTo() {
		return to;
	}
}

