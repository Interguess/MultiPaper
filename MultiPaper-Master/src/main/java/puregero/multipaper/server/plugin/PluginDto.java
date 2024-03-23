package puregero.multipaper.server.plugin;

import lombok.Builder;
import lombok.Data;

import java.io.File;

@Data
@Builder
public class PluginDto {

    private final File file;

    private final Plugin plugin;

    private final PluginDescription description;
}
