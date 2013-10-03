package net.miscjunk.fancyshop;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static ItemStack priceToItem(String price) {
        // TODO - configurable currencies
        try {
            Pattern p = Pattern.compile("([^0-9\\s]*)([0-9]+)([^0-9\\s]*)");
            Matcher m = p.matcher(price);
            m.find();
            String prefix = m.group(0);
            int cost = Integer.parseInt(m.group(2));
            String postfix = m.group(3);
            if (!prefix.isEmpty() && !postfix.isEmpty()) {
                throw new IllegalArgumentException("Can't have both prefix and postfix for currency");
            } else if (prefix.isEmpty() && postfix.isEmpty()) {
                throw new IllegalArgumentException("Must specify a currency");
            } else if (!prefix.isEmpty()) {
                throw new IllegalArgumentException("Unknown currency");
            } else if (!postfix.isEmpty()) {
                if (postfix.equals("E")) {
                    return new ItemStack(Material.EMERALD, cost);
                } else {
                    throw new IllegalArgumentException("Unknown currency");
                }
            } else {
                throw new IllegalArgumentException("Unknown currency");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown currency");
        }
    }
    public static String itemToPrice(ItemStack item) {
        // TODO - configurable currencies
        if (item.getType() == Material.EMERALD) {
            return item.getAmount()+"E";
        } else {
            return item.getAmount() + " " + itemName(item);
        }
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

    public static boolean isCurrency(ItemStack item) {
        // TODO - configurable currencies
        if (item.hasItemMeta()) return false;
        if (item.getData().getData() != 0) return false;
        return true;
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
