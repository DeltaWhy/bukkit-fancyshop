package net.miscjunk.fancyshop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Deal {
    private ItemStack item;
    private int available;
    private ItemStack price;

    public Deal(ItemStack item, int available, String price) {
        this(item, available, priceToItem(price));
    }
    public Deal(ItemStack item, String price) {
        this(item, priceToItem(price));
    }
    public Deal(ItemStack item, ItemStack price) {
        this(item, 0, price);
    }
    private static ItemStack priceToItem(String price) {
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
    private static String itemToPrice(ItemStack item) {
        // TODO - configurable currencies
        if (item.getType() == Material.EMERALD) {
            return item.getAmount()+"E";
        } else {
            return item.getAmount() + " " + item.getType();
        }
    }

    public Deal(ItemStack item, int available, ItemStack price) {
        this.item = item;
        this.available = available;
        this.price = price;
    }

    public List<String> toLore() {
        List<String> lore = new ArrayList<String>();
        lore.add(""+ChatColor.RESET+ChatColor.GREEN+itemToPrice(price));
        if (available > 0) {
            lore.add(""+available+" in stock");
        } else {
            lore.add(""+ChatColor.RED+"Out of stock!");
        }
        return lore;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getPrice() {
        return price;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }
}
