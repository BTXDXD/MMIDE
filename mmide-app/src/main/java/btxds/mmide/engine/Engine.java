package btxds.mmide.engine;

import btxds.mmide.api.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class Engine {

    public static final List<Class<?>> registeredPlugins = new ArrayList<>();
    public static URLClassLoader pluginClassLoader;

    public static void loadPlugins() {
        registeredPlugins.clear();

        Path pluginsDir = Path.of("plugins");

        try {
            if (!Files.exists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
                System.out.println("[Engine] Directory 'plugins' created.");
                return;
            }
        } catch (IOException e) {
            System.err.println("[Engine] Failed to create plugins directory: " + e.getMessage());
            return;
        }

        List<File> jarFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pluginsDir)) {
            stream.filter(path -> path.toString().endsWith(".jar"))
                    .map(Path::toFile)
                    .forEach(jarFiles::add);
        } catch (IOException e) {
            System.err.println("[Engine] Failed to read plugins directory: " + e.getMessage());
            return;
        }

        if (jarFiles.isEmpty()) {
            System.out.println("[Engine] No .jar plugins found in 'plugins' directory.");
            return;
        }

        URL[] urls = jarFiles.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);

        pluginClassLoader = new URLClassLoader(urls, Engine.class.getClassLoader());

        for (File jarFile : jarFiles) scanJar(jarFile, pluginClassLoader);

        System.out.println("[Engine] Successfully loaded " + registeredPlugins.size() + " plugin(s).");
    }

    private static void scanJar(File file, ClassLoader classLoader) {
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
                            registeredPlugins.add(clazz);

                            System.out.println("[Engine] Registered plugin: " +
                                    (info.name().isEmpty() ? info.id() : info.name()) +
                                    " (id: " + info.id() + ", isLoader: " + info.isLoader() + ")");
                        }
                    } catch (Throwable ignored) {
                        // Пропускаем классы сторонних библиотек, если у них нет зависимостей в рантайме
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Engine] Could not read jar: " + file.getName() + " (" + e.getMessage() + ")");
        }
    }

}