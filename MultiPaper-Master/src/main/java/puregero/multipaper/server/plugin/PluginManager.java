package puregero.multipaper.server.plugin;

import de.interguess.igdatastores.Datastore;
import de.interguess.igdatastores.impl.DatastoreImpl;
import de.interguess.javamoduleloader.module.Module;
import de.interguess.javamoduleloader.module.loader.ModuleLoader;
import de.interguess.javamoduleloader.module.loader.SimpleModuleLoader;
import lombok.extern.slf4j.Slf4j;
import org.simpleyaml.configuration.file.YamlConfiguration;
import puregero.multipaper.server.Constants;
import puregero.multipaper.server.exception.PluginDisableException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class PluginManager {

    private final ModuleLoader<Plugin> pluginLoader;

    private final Datastore<PluginDto> datastore;

    public PluginManager() {
        this.pluginLoader = new SimpleModuleLoader<>();
        this.datastore = new DatastoreImpl<>();
    }

    public List<Plugin> getEnabledPlugins() {
        return datastore.listAll().stream().map(PluginDto::getPlugin).toList();
    }

    public void loadPlugins() {
        File pluginsFolder = new File("plugins");

        if (!pluginsFolder.exists()) {
            try {
                log.info("Creating plugin directory...");

                Files.createDirectory(pluginsFolder.toPath());

                log.info("Created plugin directory");
            } catch (IOException e) {
                log.error("Failed to create plugins folder, create it manually if you want to use plugins!", e);
            }
        }

        long start = System.currentTimeMillis();
        
        log.info("Loading plugins...");

        File[] files = pluginsFolder.listFiles();

        if (files != null) {
            Arrays.stream(files).forEach(this::enablePlugin);
        }

        log.info("Loaded {} plugins in {}ms", datastore.size(), System.currentTimeMillis() - start);
    }

    public void enablePlugin(File file) {
        if (datastore.createQuery().field("file").equal(file).get() != null) {
            throw new IllegalArgumentException("Plugin is already enabled");
        }

        if (file.getName().endsWith(".jar")) {
            Module<Plugin> pluginModule;

            try {
                pluginModule = pluginLoader.loadModuleByFile(file);
            } catch (Exception e) {
                log.error("Failed to load module for plugin {}", file.getName(), e);

                return;
            }

            try (InputStream pluginDescriptionStream = pluginModule.getResourceAsStream("plugin.yml")) {
                if (pluginDescriptionStream == null) {
                    log.error("Failed to load {}! Jar does not contain plugin.yml", file.getName());

                    return;
                }

                YamlConfiguration pluginDescriptionConfig;

                try {
                    pluginDescriptionConfig = YamlConfiguration.loadConfiguration(pluginDescriptionStream);
                } catch (Exception e) {
                    log.error("Failed to load {}! Failed to parse plugin.yml. Does your plugin.yml contain a error?", file.getName(), e);

                    return;
                }

                String name = pluginDescriptionConfig.getString("name");

                if (name == null) {
                    log.error("Failed to load {}! Plugin name is missing in plugin.yml", file.getName());

                    return;
                }

                String main = pluginDescriptionConfig.getString("main");

                if (main == null) {
                    log.error("Failed to load {}! Plugin main is missing in plugin.yml", file.getName());

                    return;
                }

                String version = pluginDescriptionConfig.getString("version");

                if (version == null) {
                    log.error("Failed to load {}! Plugin version is missing in plugin.yml", file.getName());

                    return;
                }

                String description = pluginDescriptionConfig.getString("description", "No description provided");

                List<String> authors = pluginDescriptionConfig.getStringList("authors");

                if (authors == null || authors.isEmpty()) {
                    authors = Collections.singletonList("Unknown");
                }

                List<String> loadBefore = pluginDescriptionConfig.getStringList("load-before");

                if (loadBefore == null) {
                    loadBefore = Collections.emptyList();
                }

                List<String> depends = pluginDescriptionConfig.getStringList("depends");

                if (depends == null) {
                    depends = Collections.emptyList();
                }

                PluginDescription pluginDescription = PluginDescription.builder()
                        .name(name)
                        .main(main)
                        .version(version)
                        .description(description)
                        .authors(authors)
                        .loadBefore(loadBefore)
                        .depends(depends)
                        .build();


                Class<Plugin> mainClass;

                try {
                    mainClass = pluginModule.getMainClassByName(Plugin.class, main);
                } catch (Exception e) {
                    log.error("Failed to load {}! Main class {} not found", file.getName(), main, e);

                    return;
                }

                log.info("Enabling plugin {}...", file.getName());

                try {
                    Plugin plugin = mainClass.getDeclaredConstructor().newInstance();

                    plugin.onEnable();

                    datastore.saveAll(
                            PluginDto.builder()
                                    .file(file)
                                    .plugin(plugin)
                                    .description(pluginDescription)
                                    .build()
                    );

                    log.info("Enabled plugin {}!", file.getName());
                } catch (Exception e) {
                    log.error("Failed to enable plugin {}", file.getName(), e);
                }
            } catch (Exception e) {
                log.error("Failed to load plugin {}", file.getName(), e);
            }
        }
    }

    public void disablePlugin(Plugin plugin) {
        PluginDto pluginDto = datastore.createQuery()
                .field("plugin").equal(plugin)
                .get();

        if (pluginDto == null) {
            throw new PluginDisableException("Plugin is not enabled");
        }

        plugin.onDisable();

        datastore.deleteAll(pluginDto);

        try {
            pluginLoader.unloadModuleByFile(pluginDto.getFile());
        } catch (Exception e) {
            throw new PluginDisableException("Failed to disable plugin", e);
        }
    }
}
