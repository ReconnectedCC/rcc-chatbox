package cc.reconnected.chatbox.utils;

import cc.reconnected.chatbox.ClientPacketsHandler;
import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.license.License;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class Webhook {
    private final static HttpClient http = HttpClient.newHttpClient();

    private static void send(JsonObject body) {
        var url = RccChatbox.CONFIG.webhook;
        if (url == null) return;

        try {
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                RccChatbox.LOGGER.error("Received {} as status code!\n{}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            RccChatbox.LOGGER.error("Could not send webhook message!", e);
        }
    }

    public static void send(UUID licenseId, ClientPacketsHandler.ClientMessage message, @Nullable ServerPlayer recipient) {
        var license = RccChatbox.licenseManager().getLicense(licenseId);
        String label = null;
        if (message.label() != null) {
            label = PlainTextComponentSerializer.plainText().serialize(message.label());
        }
        var text = PlainTextComponentSerializer.plainText().serialize(message.message());

        String recipientName = null;
        if (recipient != null) {
            recipientName = recipient.getGameProfile().getName();
        }

        var json = build(license, label, text, message.type(), recipientName);
        send(json);
    }

    public static JsonObject build(License license, String label, String content, ClientPacketsHandler.MessageTypes type, @Nullable String toUser) {
        var json = new JsonObject();

        json.addProperty("flags", 4096); // no mention

        var embed = new JsonObject();
        embed.addProperty("title", label != null ? label : license.user.name);
        embed.addProperty("description", content);

        var color = switch (type) {
            case TELL -> NamedTextColor.BLUE.value();
            case SAY -> NamedTextColor.GOLD.value();
        };
        embed.addProperty("color", color);

        var fields = new JsonArray();
        var typeField = new JsonObject();
        typeField.addProperty("name", "Type");
        typeField.addProperty("value", type.toString());
        typeField.addProperty("inline", true);

        var licenseField = new JsonObject();
        licenseField.addProperty("name", "License");
        licenseField.addProperty("value", license.uuid().toString());
        licenseField.addProperty("inline", true);

        fields.add(typeField);
        fields.add(licenseField);

        if (toUser != null) {
            var userField = new JsonObject();
            userField.addProperty("name", "Recipient");
            userField.addProperty("value", toUser);
            userField.addProperty("inline", true);

            fields.add(userField);
        }

        var author = new JsonObject();
        author.addProperty("name", String.format("%s (%s)", license.user.name, license.user.uuid));

        embed.add("fields", fields);
        embed.add("author", author);

        var embeds = new JsonArray();
        embeds.add(embed);
        json.add("embeds", embeds);

        return json;
    }
}
