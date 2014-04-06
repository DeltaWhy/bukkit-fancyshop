package net.miscjunk.fancyshop;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Deal {
    private ItemStack item;
    private int available;
    private int buying;
    private ItemStack buyPrice;
    private ItemStack sellPrice;

    public Deal(ItemStack item) {
        this(item, (ItemStack)null, (ItemStack)null);
    }

    public Deal(ItemStack item, ItemStack buyPrice, ItemStack sellPrice) {
        this.item = item;
        this.available = 0;
        this.buying = 0;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public void setBuyPrice(ItemStack buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setSellPrice(ItemStack sellPrice) {
        this.sellPrice = sellPrice;
    }

    public List<String> toLore(boolean admin) {
        List<String> lore = new ArrayList<String>();
        if (buyPrice != null)
            lore.add(""+ChatColor.RESET+ChatColor.GREEN+"Buy: "+ CurrencyManager.getInstance().itemToPrice(buyPrice));
        if (sellPrice != null)
            lore.add(""+ChatColor.RESET+ChatColor.BLUE+"Sell: "+ CurrencyManager.getInstance().itemToPrice(sellPrice));
        if (buyPrice != null && !admin) {
            if (available > 0) {
                lore.add(""+available+" in stock");
            } else {
                lore.add(""+ChatColor.RED+"Out of stock!");
            }
        }
        if (sellPrice != null && !admin) {
            if (buying > 0) {
                lore.add("Buying "+buying);
            } else {
                lore.add("Not buying");
            }
        }
        if (buyPrice == null && sellPrice == null) {
            lore.add(""+ChatColor.RESET+ChatColor.RED+"Price not set");
        }
        return lore;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getBuyPrice() {
        return buyPrice;
    }

    public ItemStack getSellPrice() {
        return sellPrice;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getBuying() {
        return buying;
    }

    public void setBuying(int buying) {
        this.buying = buying;
    }
}
