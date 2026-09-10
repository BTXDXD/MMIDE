package btxds.mmide.engine;

import btxds.mmide.api.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Engine {

    public static final List<Class<?>> registeredPlugins = new ArrayList<>();

    public static final Map<String, IPlugin> activePlugins = new HashMap<>();
    public static final Map<String, LoaderPlugin> loaderPlugins = new HashMap<>();
    public static final Map<String, List<Block>> loaderBlocks = new HashMap<>();

    public static URLClassLoader pluginClassLoader;

    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    private record DiscoveredPlugin(String id, String name, boolean isLoader, String jarFileName, Class<?> clazz) {}

    public static boolean loadPlugins() {
        registeredPlugins.clear();
        activePlugins.clear();
        loaderPlugins.clear();
        loaderBlocks.clear();

        Path pluginsDir = Path.of("plugins");

        try {
            if (!Files.exists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
                System.out.println("[Engine] Directory 'plugins' created.");
                return true;
            }
        } catch (IOException e) {
            System.err.println("[Engine] Failed to create plugins directory: " + e.getMessage());
            return true;
        }

        List<File> jarFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pluginsDir)) {
            stream.filter(path -> path.toString().endsWith(".jar"))
                    .map(Path::toFile)
                    .forEach(jarFiles::add);
        } catch (IOException e) {
            System.err.println("[Engine] Failed to read plugins directory: " + e.getMessage());
            return true;
        }

        if (jarFiles.isEmpty()) {
            System.out.println("[Engine] No .jar plugins found in 'plugins' directory.");
            return true;
        }

        URL[] urls = jarFiles.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);

        pluginClassLoader = new URLClassLoader(urls, Engine.class.getClassLoader());

        List<DiscoveredPlugin> discovered = new ArrayList<>();

        for (File jarFile : jarFiles) scanJar(jarFile, pluginClassLoader, discovered);

        List<String> invalidIdErrors = new ArrayList<>();
        for (DiscoveredPlugin plugin : discovered) {
            if (plugin.id().isEmpty() || !VALID_ID_PATTERN.matcher(plugin.id()).matches()) {
                invalidIdErrors.add("Invalid ID: '" + plugin.id() + "' in file: " + plugin.jarFileName() +
                        "\n  (Allowed: lowercase 'a-z', numbers '0-9', '_' and '-')");
            }
        }

        if (!invalidIdErrors.isEmpty()) {
            showErrorDialog(
                    "Invalid Plugin ID Format",
                    "One or more plugins have an invalid ID format:\n\n"
                            + String.join("\n\n", invalidIdErrors)
                            + "\n\nPlease correct the plugin IDs or remove invalid JARs."
            );
            return false;
        }

        Map<String, List<DiscoveredPlugin>> pluginsById = discovered.stream()
                .collect(Collectors.groupingBy(DiscoveredPlugin::id));

        List<String> duplicateErrors = new ArrayList<>();
        for (Map.Entry<String, List<DiscoveredPlugin>> entry : pluginsById.entrySet()) {
            List<DiscoveredPlugin> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("Duplicate ID: '").append(entry.getKey()).append("'\n");
                for (DiscoveredPlugin dup : duplicates) {
                    sb.append("   - File: ").append(dup.jarFileName())
                            .append(" (Class: ").append(dup.clazz().getName()).append(")\n");
                }
                duplicateErrors.add(sb.toString().trim());
            }
        }

        if (!duplicateErrors.isEmpty()) {
            showErrorDialog(
                    "Plugin Conflict Error",
                    "Duplicate plugin IDs detected!\n\n"
                            + String.join("\n\n", duplicateErrors)
                            + "\n\nPlease remove conflicting plugin JARs from the 'plugins' directory."
            );
            return false;
        }

        for (DiscoveredPlugin p : discovered) {
            registeredPlugins.add(p.clazz());
            if (!initPluginInstance(p)) return false;
        }

        System.out.println("[Engine] Successfully loaded and initialized " + activePlugins.size() + " plugin(s).");
        return true;
    }

    private static boolean initPluginInstance(DiscoveredPlugin p) {
        try {
            Object instance = p.clazz().getDeclaredConstructor().newInstance();

            if (instance instanceof IPlugin plugin) {
                PluginContext context = new PluginContext() {
                    @Override
                    public String getPluginId() {
                        return p.id();
                    }

                    @Override
                    public void log(String message) {
                        System.out.println("[" + p.name() + "] " + message);
                    }

                    @Override
                    public void logError(String message, Throwable throwable) {
                        System.err.println("[" + p.name() + "] ERROR: " + message);
                        if (throwable != null) throwable.printStackTrace();
                    }

                    @Override
                    public File getDataFolder() {
                        File dir = new File("plugins/data", p.id());
                        dir.mkdirs();
                        return dir;
                    }
                };

                plugin.onLoad(context);
                activePlugins.put(p.id(), plugin);

                if (plugin instanceof LoaderPlugin loader) {
                    loaderPlugins.put(p.id(), loader);

                    SimpleBlockRegistry registry = new SimpleBlockRegistry();
                    try {
                        loader.registerBlocks(registry);
                    } catch (IllegalArgumentException ex) {
                        showErrorDialog(
                                "Plugin Block Error",
                                "Failed to initialize loader plugin '" + p.name() + "' (" + p.jarFileName() + "):\n\n"
                                        + ex.getMessage()
                        );
                        return false;
                    }

                    loaderBlocks.put(p.id(), new ArrayList<>(registry.getAllBlocks()));
                }

                System.out.println("[Engine] Initialized: " + p.name() + " (id: " + p.id() + ")");
                return true;
            } else {
                System.err.println("[Engine] Class " + p.clazz().getName() + " has @Plugin but does not implement IPlugin!");
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Engine] Failed to instantiate plugin " + p.id() + " from " + p.jarFileName());
            e.printStackTrace();
            return false;
        }
    }

    private static void scanJar(File file, ClassLoader classLoader, List<DiscoveredPlugin> outList) {
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory() && name.endsWith(".class")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);

                    try {
                        Class<?> clazz = Class.forName(className, false, classLoader);

                        if (clazz.isAnnotationPresent(Plugin.class)) {
                            Plugin info = clazz.getAnnotation(Plugin.class);
                            String displayName = info.name().isEmpty() ? info.id() : info.name();

                            outList.add(new DiscoveredPlugin(
                                    info.id().trim(),
                                    displayName,
                                    info.isLoader(),
                                    file.getName(),
                                    clazz
                            ));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Engine] Could not read jar: " + file.getName() + " (" + e.getMessage() + ")");
        }
    }

    private static void showErrorDialog(String title, String message) {
        System.err.println("[Engine] " + title + ":\n" + message);
        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static LoaderPlugin getLoader(String loaderId) {
        return loaderPlugins.get(loaderId);
    }

    public static List<Block> getBlocksForLoader(String loaderId) {
        return loaderBlocks.getOrDefault(loaderId, Collections.emptyList());
    }

}