package puregero.multipaper.server.handlers;

import lombok.extern.slf4j.Slf4j;
import puregero.multipaper.mastermessagingprotocol.messages.masterbound.WriteAdvancementsMessage;
import puregero.multipaper.mastermessagingprotocol.messages.serverbound.BooleanMessageReply;
import puregero.multipaper.server.FileLocker;
import puregero.multipaper.server.ServerConnection;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WriteAdvancementsHandler {

    public static void handle(ServerConnection connection, WriteAdvancementsMessage message) {
        CompletableFuture.runAsync(() -> {
            try {
                FileLocker.writeBytes(new File(new File(message.world, "advancements"), message.uuid + ".json"), message.data);

                connection.sendReply(new BooleanMessageReply(true), message);
            } catch (IOException e) {
                log.error("Failed to write advancements", e);
            }
        });
    }
}
