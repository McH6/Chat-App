package exception;

public class ChatMessageException extends Exception {
	private static final long serialVersionUID = 4971318774158662861L;

	public ChatMessageException(String msg) {
		super(msg);
	}
}
