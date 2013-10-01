package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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
        deals.add(new Deal(new ItemStack(Material.COBBLESTONE, 32), new ItemStack(Material.EMERALD, 2), new ItemStack(Material.IRON_INGOT, 2)));
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
        deals.add(new Deal(bow, new ItemStack(Material.DIAMOND, 2), null));
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta)book.getItemMeta();
        meta.addStoredEnchant(Enchantment.ARROW_DAMAGE, 1, false);
        book.setItemMeta(meta);
        deals.add(new Deal(book, new ItemStack(Material.DIAMOND, 1), null));
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
        viewInv.clear();
        for (int i=0, j=0; i < sourceInv.getSize() && j < viewInv.getSize(); i++) {
            ItemStack it = sourceInv.getItem(i);
            if (it == null) continue;
            Deal deal = null;
            for (Deal d : deals) {
                if (d.getItem().isSimilar(it) || ((d.getSellPrice() != null) && d.getSellPrice().isSimilar(it))) { //&& sourceInv.containsAtLeast(d.getItem(), d.getItem().getAmount())) {
                    deal = d;
                    deal.setAvailable(countItems(sourceInv, deal.getItem()));
                    int currency = countItems(sourceInv, deal.getSellPrice());
                    deal.setBuying(deal.getItem().getAmount() * currency/deal.getSellPrice().getAmount());
                    break;
                }
            }
            if (deal == null || dealMap.containsValue(deal)) continue;
            List<String> lore = deal.toLore();
            ItemStack view = deal.getItem().clone();
            ItemMeta meta = view.getItemMeta();
            meta.setLore(lore);
            view.setItemMeta(meta);
            dealMap.put(j, deal);
            viewInv.setItem(j, view);
            j++;
        }
    }
    public void refreshDeals() {
        for (Map.Entry<Integer, Deal> d : dealMap.entrySet()) {
            Deal deal = d.getValue();
            deal.setAvailable(countItems(sourceInv, deal.getItem()));
            int currency = countItems(sourceInv, deal.getSellPrice());
            deal.setBuying(deal.getItem().getAmount() * currency/deal.getSellPrice().getAmount());
            ItemStack view = viewInv.getItem(d.getKey());
            ItemMeta meta = view.getItemMeta();
            meta.setLore(deal.toLore());
            view.setItemMeta(meta);
        }
    }

    private int countItems(Inventory inv, ItemStack it) {
        int count = 0;
        for (ItemStack i : inv.getContents()) {
            if (i != null && i.isSimilar(it)) count += i.getAmount();
        }
        return count;
    }
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getRawSlot() < event.getInventory().getSize()) {
            // click in shop
            switch (event.getAction()) {
                case SWAP_WITH_CURSOR:
                    event.setCancelled(true);
                    Deal deal = dealMap.get(event.getSlot());
                    if (deal != null) {
                        if (deal.getBuyPrice() != null && deal.getBuyPrice().isSimilar(event.getCursor())) {
                            buy(event.getWhoClicked(), deal, event.getView());
                        } else if (deal.getSellPrice() != null && deal.getItem().isSimilar(event.getCursor())) {
                            sell(event.getWhoClicked(), deal, event.getView());
                        }
                    } else {
                        Bukkit.broadcastMessage("No deal found");
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    event.setCancelled(true);
                    deal = dealMap.get(event.getSlot());
                    if (deal != null) {
                        if (deal.getBuyPrice() != null && deal.getBuyPrice().isSimilar(event.getCursor())) {
                            buyAll(event.getWhoClicked(), deal, event.getView());
                        } else if (deal.getSellPrice() != null && deal.getItem().isSimilar(event.getCursor())) {
                            sellAll(event.getWhoClicked(), deal, event.getView());
                        }
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
                    event.setCancelled(true);
                    break;
                case MOVE_TO_OTHER_INVENTORY:
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

    private void sellAll(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        while (deal.getItem().isSimilar(cursor) && deal.getItem().getAmount() <= cursor.getAmount()) {
            if (!sell(whoClicked, deal, view)) break;
        }
    }


    private boolean sell(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        if (deal.getItem().isSimilar(cursor)) {
            if (deal.getItem().getAmount() > cursor.getAmount()) {
                Bukkit.broadcastMessage("Not enough item");
                return false;
            } else {
                if (!sourceInv.containsAtLeast(deal.getSellPrice(), deal.getSellPrice().getAmount())) {
                    Bukkit.broadcastMessage("Out of money");
                    return false;
                } else {
                    Bukkit.broadcastMessage("Selling");
                    // try depositing item
                    Map<Integer, ItemStack> overflow = sourceInv.addItem(deal.getItem().clone());
                    if (!overflow.isEmpty()) {
                        Bukkit.broadcastMessage("No room for item");
                        sourceInv.removeItem(deal.getItem().clone());
                        return false;
                    }
                    sourceInv.removeItem(deal.getSellPrice());
                    cursor.setAmount(cursor.getAmount()-deal.getItem().getAmount());
                    if (cursor.getAmount() == 0) {
                        Bukkit.broadcastMessage("Placing in hand");
                        view.setCursor(deal.getSellPrice().clone());
                    } else {
                        Bukkit.broadcastMessage("Placing in inventory");
                        overflow = whoClicked.getInventory().addItem(deal.getSellPrice().clone());
                        for (ItemStack it : overflow.values()) {
                            Bukkit.broadcastMessage("Dropping");
                            whoClicked.getWorld().dropItemNaturally(whoClicked.getLocation(), it);
                        }
                    }
                    refreshDeals();
                    return true;
                }
            }
        } else {
            Bukkit.broadcastMessage("Not holding item");
            return false;
        }
    }

    private void buyAll(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        while (deal.getBuyPrice().isSimilar(cursor) && deal.getBuyPrice().getAmount() <= cursor.getAmount()) {
            if (!buy(whoClicked, deal, view)) break;
        }
    }

    private boolean buy(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        if (deal.getBuyPrice().isSimilar(cursor)) {
            if (deal.getBuyPrice().getAmount() > cursor.getAmount()) {
                Bukkit.broadcastMessage("Not enough money");
                return false;
            } else {
                if (!sourceInv.containsAtLeast(deal.getItem(), deal.getItem().getAmount())) {
                    Bukkit.broadcastMessage("Out of stock");
                    return false;
                } else {
                    Bukkit.broadcastMessage("Buying");
                    // try depositing currency
                    Map<Integer, ItemStack> overflow = sourceInv.addItem(deal.getBuyPrice().clone());
                    if (!overflow.isEmpty()) {
                        Bukkit.broadcastMessage("No room for currency");
                        sourceInv.removeItem(deal.getBuyPrice().clone());
                        return false;
                    }
                    sourceInv.removeItem(deal.getItem());
                    cursor.setAmount(cursor.getAmount()-deal.getBuyPrice().getAmount());
                    if (cursor.getAmount() == 0) {
                        Bukkit.broadcastMessage("Placing in hand");
                        view.setCursor(deal.getItem().clone());
                    } else {
                        Bukkit.broadcastMessage("Placing in inventory");
                        overflow = whoClicked.getInventory().addItem(deal.getItem().clone());
                        for (ItemStack it : overflow.values()) {
                            Bukkit.broadcastMessage("Dropping");
                            whoClicked.getWorld().dropItemNaturally(whoClicked.getLocation(), it);
                        }
                    }
                    refreshDeals();
                    return true;
                }
            }
        } else {
            Bukkit.broadcastMessage("Not holding currency");
            return false;
        }
    }
}