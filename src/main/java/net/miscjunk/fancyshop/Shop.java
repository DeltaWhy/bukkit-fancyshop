package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shop implements InventoryHolder {
    Inventory sourceInv;
    Inventory viewInv;
    String owner;
    List<Deal> deals;
    Map<Integer, Deal> dealMap;

    private Shop(Inventory inv, String owner) {
        this.owner = owner;
        sourceInv = inv;
        viewInv = Bukkit.createInventory(this, 27, owner+"'s Shop");
        // TODO - custom deals
        deals = new ArrayList<Deal>();
        deals.add(new Deal(new ItemStack(Material.COBBLESTONE, 64), 640, new ItemStack(Material.EMERALD, 1)));
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
        dealMap = new HashMap<Integer, Deal>();
        for (int i=0; i < sourceInv.getSize() && i < viewInv.getSize(); i++) {
            ItemStack it = sourceInv.getItem(i);
            if (it == null) continue;
            it = it.clone();
            Deal deal = null;
            for (Deal d : deals) {
                if (d.getItem().isSimilar(it)) {
                    deal = d;
                    break;
                }
            }
            List<String> lore;
            if (deal != null) {
                lore = deal.toLore();
                dealMap.put(i, deal);
            } else {
                lore = new ArrayList<String>();
                lore.add(ChatColor.COLOR_CHAR+"r"+"Shop Item");
            }
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
                    Deal deal = dealMap.get(event.getSlot());
                    if (deal != null) {
                        buy(event.getWhoClicked(), deal, event.getView());
                    } else {
                        Bukkit.broadcastMessage("No deal found");
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    Bukkit.broadcastMessage("shift-click");
                    event.setCancelled(true);
                    deal = dealMap.get(event.getSlot());
                    if (deal != null) {
                        buyAll(event.getWhoClicked(), deal, event.getView());
                    } else {
                        Bukkit.broadcastMessage("No deal found");
                    }
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

    private void buyAll(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        while (deal.getPrice().isSimilar(cursor) && deal.getPrice().getAmount() <= cursor.getAmount()) {
            if (!buy(whoClicked, deal, view)) break;
        }
    }

    private boolean buy(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        if (deal.getPrice().isSimilar(cursor)) {
            if (deal.getPrice().getAmount() > cursor.getAmount()) {
                Bukkit.broadcastMessage("Not enough money");
                return false;
            } else {
                if (!sourceInv.containsAtLeast(deal.getItem(), deal.getItem().getAmount())) {
                    Bukkit.broadcastMessage("Out of stock");
                    return false;
                } else {
                    Bukkit.broadcastMessage("Buying");
                    sourceInv.removeItem(deal.getItem());
                    cursor.setAmount(cursor.getAmount()-deal.getPrice().getAmount());
                    if (cursor.getAmount() == 0) {
                        Bukkit.broadcastMessage("Placing in hand");
                        view.setCursor(deal.getItem().clone());
                    } else {
                        Bukkit.broadcastMessage("Placing in inventory");
                        Map<Integer, ItemStack> overflow = whoClicked.getInventory().addItem(deal.getItem().clone());
                        for (ItemStack it : overflow.values()) {
                            Bukkit.broadcastMessage("Dropping");
                            whoClicked.getWorld().dropItemNaturally(whoClicked.getLocation(), it);
                        }
                    }
                    return true;
                }
            }
        } else {
            Bukkit.broadcastMessage("Not holding currency");
            return false;
        }
    }
}
