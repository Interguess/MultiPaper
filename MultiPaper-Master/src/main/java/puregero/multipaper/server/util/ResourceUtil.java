package puregero.multipaper.server.util;


import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

@UtilityClass
public class ResourceUtil {

    public static boolean existsResource(@NotNull ClassLoader classLoader, @NotNull String path) {
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            return inputStream != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static @Nullable String readResource(@NotNull ClassLoader classLoader, @NotNull String path) {
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                return null;
            }

            return new String(inputStream.readAllBytes());
        } catch (Exception ignored) {
            return null;
        }
    }
}
