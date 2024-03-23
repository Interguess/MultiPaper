package puregero.multipaper.server.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.file.YamlConfiguration;
import puregero.multipaper.server.exception.ConfigException;
import puregero.multipaper.server.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Getter
public class Config {

    private final String name;

    private final File configFile;

    private final YamlConfiguration config;

    public Config(@Nullable File parent, @NotNull String name) {
        this.name = name;
        this.configFile = new File(parent == null ? new File("") : parent, name);
        this.config = new YamlConfiguration();

        parent.mkdirs();

        this.initialize();
    }

    private void initialize() {
        if (!configFile.exists()) {
            String path = configFile.getName();

            path = path.startsWith("/") ? path : "/" + path;

            if (ResourceUtil.existsResource(getClass().getSuperclass().getClassLoader(), path)) {
                try {
                    Files.createFile(configFile.toPath());

                    String content = ResourceUtil.readResource(getClass().getSuperclass().getClassLoader(), path);

                    assert content != null; //should never be null

                    Files.writeString(configFile.toPath(), content);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    if (!configFile.createNewFile()) {
                        throw new ConfigException(String.format("Failed to create config %s!", name));
                    }
                } catch (Exception e) {
                    throw new ConfigException(String.format("Failed to create config %s!", name), e);
                }
            }
        }

        load();
    }

    public void load() {
        try {
            config.load(configFile);
        } catch (Exception e) {
            throw new ConfigException(String.format("Failed to load config %s!", name), e);
        }
    }
}