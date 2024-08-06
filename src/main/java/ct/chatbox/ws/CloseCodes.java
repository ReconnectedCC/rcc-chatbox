package ct.chatbox.ws;

public enum CloseCodes {
    SERVER_STOPPING(4000, "Server is restarting, please reconnect in a few minutes"),
    EXTERNAL_GUESTS_NOT_ALLOWED(4001, "External guests are not allowed"),
    UNKNOWN_LICENSE_KEY(4002, "Unknown license key. Get one with /chatbox license register"),
    INVALID_LICENSE_KEY(4003, "Invalid license key. Get one with /chatbox license register"),
    DISABLED_LICENSE(4004, "Your license has been disabled. Please contact a member of staff"),
    CHANGED_LICENSE_KEY(4005, "Your license key was invalidated"),
    FATAL_ERROR(4006, "A fatal error occurred"),
    UNSUPPORTED_ENDPOINT(4007, "Unsupported websocket endpoint. Supported endpoints: /v2/:token");

    public final int code;
    public final String message;

    CloseCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getErrorString() {
        return this.toString().toLowerCase();
    }
}
