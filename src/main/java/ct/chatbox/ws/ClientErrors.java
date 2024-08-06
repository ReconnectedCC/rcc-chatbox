package ct.chatbox.ws;

public enum ClientErrors {
    UNKNOWN_ERROR("An unknown error occurred."),
    UNKNOWN_TYPE("Unrecognised message type.");

    public final String message;
    ClientErrors(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return this.toString().toLowerCase();
    }
}
