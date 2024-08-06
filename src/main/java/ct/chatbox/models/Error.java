package ct.chatbox.models;

import org.jetbrains.annotations.Nullable;

public class Error {
    public String type = "error";
    public boolean ok = false;
    public String error;
    public String message;
    public int id = -1;

    public Error(String error, String message, @Nullable Integer id) {
        this.error = error;
        this.message = message;
        if (id != null) {
            this.id = id;
        }
    }
}
