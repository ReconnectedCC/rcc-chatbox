package cc.reconnected.chatbox;

import cc.reconnected.server.RccServer;

import java.sql.*;

public class ChatboxDatabase {
    public Connection connection() throws SQLException {
        return RccServer.getInstance().database().connection();
    }
    /*
        uuid: varchar/uuid license key
        userId: varchar/uuid mc player uuid
        capabilities: int bit flags
        timestamp: creation date
     */
    public void ensureDatabaseCreated() {

        try(final var conn = connection()) {
            var statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS chatbox_licenses (" +
                    "uuid UUID PRIMARY KEY," +
                    "userId UUID NOT NULL UNIQUE," +
                    "capabilities INT NOT NULL DEFAULT 0," +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");
            statement.close();
            Chatbox.LOGGER.info("Chatbox table created!");
        } catch(SQLException e) {
            Chatbox.LOGGER.error("Could not create chatbox table", e);
        }
    }
}
