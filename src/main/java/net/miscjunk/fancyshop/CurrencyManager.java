package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyManager {
    private static CurrencyManager instance;
    FancyShop plugin;
    boolean whitelist;
    Map<String,ItemStack> currencies;
    List<Integer> blacklist;

    public static void init(FancyShop plugin) {
        if (instance != null) throw new RuntimeException("CurrencyManager is already initialized");
        instance = new CurrencyManager(plugin);
    }

    public static CurrencyManager getInstance() {
        if (instance == null) throw new RuntimeException("CurrencyManager is not initialized");
        return instance;
    }

    private CurrencyManager(FancyShop plugin) {
        this.plugin = plugin;
        whitelist = plugin.getConfig().getBoolean("currency-whitelist");
        blacklist = plugin.getConfig().getIntegerList("currency-blacklist");
        currencies = new HashMap<String, ItemStack>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("currencies");
        for (String name : section.getKeys(false)) {
            String value = section.getString(name);
            if (value.startsWith("item:\n")) {
                // it's an NBT dump
                currencies.put(name, stringToItem(value));
            } else {
                // it's data[:damage]
                Pattern p = Pattern.compile("([0-9]+)(:([0-9]+))?");
                Matcher m = p.matcher(value);
                m.find();
                int data = Integer.parseInt(m.group(1));
                int damage = 0;
                if (m.group(2) != null) damage = Integer.parseInt(m.group(3));
                currencies.put(name, new ItemStack(data, 1, (short)damage));
            }
        }
    }

    public String itemToPrice(ItemStack item) {
        for (Map.Entry<String,ItemStack> e : currencies.entrySet()) {
            if (e.getValue().isSimilar(item)) {
                return item.getAmount() + " " + e.getKey();
            }
        }
        return item.getAmount() + " " + itemName(item);
    }

    public static String itemName(ItemStack item) {
        String[] words = item.getType().toString().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase()).append(' ');
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public boolean isCurrency(ItemStack item) {
        if (item == null) return false;
        if (blacklist.contains(item.getTypeId())) return false;
        for (ItemStack i : currencies.values()) {
            if (i.isSimilar(item)) return true;
        }
        if (whitelist) return false;
        if (item.hasItemMeta()) return false;
        if (item.getData().getData() != 0) return false;
        return true;
    }

    public boolean isCustomCurrency(String name) {
        return currencies.containsKey(name);
    }

    public void addCustomCurrency(String name, ItemStack item) {
        ItemStack it = item.clone();
        it.setAmount(1);
        currencies.put(name, item);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("currencies");
        section.set(name, itemToString(it));
        plugin.saveConfig();
    }

    public static String itemToString(ItemStack item) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("item", item);
        return c.saveToString();
    }

    public static ItemStack stringToItem(String str) {
        YamlConfiguration c = new YamlConfiguration();
        try {
            c.loadFromString(str);
        } catch(InvalidConfigurationException e) {
            return null;
        }
        Object o = c.get("item");
        if (!(o instanceof ItemStack)) return null;
        return (ItemStack)o;
    }
}
