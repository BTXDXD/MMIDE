package btxds.mmide.engine;

import btxds.mmide.api.models.Workspace;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class SettingsManager {

    private static final Path FILE = Path.of(System.getProperty("user.home"), ".mmide", "settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Workspace[] loaded = GSON.fromJson(reader, Workspace[].class);
            if (loaded != null) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}