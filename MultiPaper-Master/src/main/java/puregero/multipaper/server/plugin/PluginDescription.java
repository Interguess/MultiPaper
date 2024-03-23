package puregero.multipaper.server.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PluginDescription {

    private String name;

    private String main;

    private String version;

    private String description;

    private List<String> authors;

    private List<String> loadBefore;

    private List<String> depends;
}
