package net.miscjunk.fancyshop;

import java.io.File;
import java.io.InputStream;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;

public class I18n {
    private static I18n instance;

    private FancyShop plugin;
    private FileConfiguration config;
    private String locale;

    private I18n(FancyShop plugin) {
        this.plugin = plugin;

        InputStream defConfigStream = plugin.getResource("strings.yml");
        if (defConfigStream == null) throw new IllegalStateException("No strings.yml found");

        this.config = YamlConfiguration.loadConfiguration(defConfigStream);
        this.locale = plugin.getConfig().getString("locale");
    }

    private String get(String path) {
        String translation = config.getConfigurationSection(locale).getString(path);
        if (translation == null) translation = config.getConfigurationSection("en_US").getString(path);
        return translation;
    }

    public static void init(FancyShop plugin) {
        if (instance != null) throw new IllegalStateException("Already initialized I18n");
        instance = new I18n(plugin);
    }

    public static String s(String path) {
        if (instance == null) throw new IllegalStateException("I18n not initialized");
        return instance.get(path);
    }

    public static String s(String path, Object... params) {
        if (instance == null) throw new IllegalStateException("I18n not initialized");
        return String.format(instance.get(path), params);
    }

    public static String getLocale() {
        if (instance == null) throw new IllegalStateException("I18n not initialized");
        return instance.locale;
    }
}
