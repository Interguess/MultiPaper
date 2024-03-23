package puregero.multipaper.server.util;

import lombok.experimental.UtilityClass;
import puregero.multipaper.mastermessagingprotocol.ChunkKey;

@UtilityClass
public class ChunkLock {

    private static final int LOCK_COUNT = 64;
    private static final int LOCK_COUNT_MASK = 63;
    private static final Object[] locks = new Object[LOCK_COUNT];

    static {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    public static Object getChunkLock(ChunkKey key) {
        return locks[key.hashCode() & LOCK_COUNT_MASK];
    }
}
