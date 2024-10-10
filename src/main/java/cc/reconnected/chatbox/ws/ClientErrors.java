package cc.reconnected.chatbox.ws;

public enum ClientErrors {
    UNKNOWN_ERROR("An unknown error occurred."),
    UNKNOWN_TYPE("Unrecognised message type."),
    MISSING_CAPABILITY("You don't have the required capability to run this."),
    MISSING_TEXT("The 'text' argument is required."),
    MISSING_USER("The 'user' argument is required."),
    UNKNOWN_USER("That user is not online."),
    INVALID_MODE("The 'mode' argument is invalid."),
    RATE_LIMITED("You got rate limited."),
    ;

    public final String message;
    ClientErrors(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return this.toString().toLowerCase();
    }
}
