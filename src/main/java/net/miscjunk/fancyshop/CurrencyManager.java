package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtTextSerializer;

import java.io.IOException;
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
    static boolean protocolInstall = false;

    public static void init(FancyShop plugin) {
        if (instance != null) throw new RuntimeException("CurrencyManager is already initialized");
        instance = new CurrencyManager(plugin);
        protocolInstall = Bukkit.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        if(protocolInstall)
        	Bukkit.getLogger().info("Found ProtocolLib, enabling custom item data(NBT) support");
        else
        	Bukkit.getLogger().info("ProtocolLib is not installed, disabling custom item data(NBT) support");
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
        String result = c.saveToString();
        if(protocolInstall == true && item != null && item.hasItemMeta()) {
        	result += "  NBTTag: '" + ProtocolLibHook.getNbtTextSerializer(item) + "'";
        }
        return result;
    }

    public static ItemStack stringToItem(String str) {
        YamlConfiguration c = new YamlConfiguration();
        YamlConfiguration d = new YamlConfiguration();
        String NBTTag;
        NBTTag = str.replaceFirst("\n  ==: org.bukkit.inventory.ItemStack\n", "\n");
        try {
            c.loadFromString(str);
            d.loadFromString(NBTTag);
        } catch(InvalidConfigurationException e) {
            return null;
        }
        Object o = c.get("item");
        NBTTag = (String) d.get("item.NBTTag");
        if (!(o instanceof ItemStack)) return null;
        if (NBTTag == null || protocolInstall == false) return (ItemStack)o;
        ItemStack item = ProtocolLibHook.setTagFromText((ItemStack)o, NBTTag);
        return item;
    }
}

class ProtocolLibHook {
    public static ItemStack getCraftItemStack(ItemStack stack) {
        if (!MinecraftReflection.isCraftItemStack(stack))
            return MinecraftReflection.getBukkitItemStack(stack);
        else
            return stack;
    }
	
    public static String getNbtTextSerializer(ItemStack item) {
        item = ProtocolLibHook.getCraftItemStack(item);
        NbtCompound tag = NbtFactory.asCompound(NbtFactory.fromItemTag(item));
        String textwrape = new NbtTextSerializer().serialize(tag);
        return textwrape;	
    }

    public static ItemStack setTagFromText(ItemStack item, String NBTTag) {
        if(NBTTag.isEmpty())
            return item;
        item = ProtocolLibHook.getCraftItemStack(item);
        NbtCompound nbtstr = null;
        try {
            nbtstr = new NbtTextSerializer().deserializeCompound(NBTTag);
        } catch (IOException e) {
            return item;
        }
        if(!nbtstr.getKeys().isEmpty())        	
            NbtFactory.setItemTag(item, nbtstr);
        return item;
    }
}
