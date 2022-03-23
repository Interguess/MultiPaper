package puregero.multipaper.mastermessagingprotocol.messages.masterbound;

import puregero.multipaper.mastermessagingprotocol.ExtendedByteBuf;

public class UnsubscribeChunkMessage extends MasterBoundMessage {

    public final String world;
    public final int cx;
    public final int cz;

    public UnsubscribeChunkMessage(String world, int cx, int cz) {
        this.world = world;
        this.cx = cx;
        this.cz = cz;
    }

    public UnsubscribeChunkMessage(ExtendedByteBuf byteBuf) {
        world = byteBuf.readString();
        cx = byteBuf.readInt();
        cz = byteBuf.readInt();
    }

    @Override
    public void write(ExtendedByteBuf byteBuf) {
        byteBuf.writeString(world);
        byteBuf.writeInt(cx);
        byteBuf.writeInt(cz);
    }

    @Override
    public void handle(MasterBoundMessageHandler handler) {
        handler.handle(this);
    }
}
