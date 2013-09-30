package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Shop implements InventoryHolder {
    Inventory sourceInv;
    Inventory viewInv;
    String owner;
    private Shop(Inventory inv, String owner) {
        this.owner = owner;
        sourceInv = inv;
        viewInv = Bukkit.createInventory(this, 27, owner+"'s Shop");
        refreshView();
    }

    public static Shop fromInventory(Inventory inv, String owner) {
        return new Shop(inv, owner);
    }

    public void open(Player player) {
        player.openInventory(viewInv);
    }

    public Inventory getInventory() {
        return viewInv;
    }

    public void refreshView() {
        for (int i=0; i < sourceInv.getSize() && i < viewInv.getSize(); i++) {
            ItemStack it = sourceInv.getItem(i);
            if (it == null) continue;
            it = it.clone();
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.COLOR_CHAR+"r"+"Shop Item");
            ItemMeta meta = it.getItemMeta();
            meta.setLore(lore);
            it.setItemMeta(meta);
            viewInv.setItem(i, it);
        }
    }
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getRawSlot() < event.getInventory().getSize()) {
            // click in shop
            switch (event.getAction()) {
                case SWAP_WITH_CURSOR:
                    Bukkit.broadcastMessage("swap");
                    event.setCancelled(true);
                    break;
                default:
                    event.setCancelled(true);
            }
        } else {
            // click outside shop
            switch (event.getAction()) {
                case COLLECT_TO_CURSOR:
                    Bukkit.broadcastMessage("collect");
                    event.setCancelled(true);
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    Bukkit.broadcastMessage("move into");
                    if (event.getWhoClicked().getName().equalsIgnoreCase(owner)) {
                        event.setCancelled(true);
                    } else {
                        event.setCancelled(true);
                    }
                    break;
                default:
            }
        }
    }
}
