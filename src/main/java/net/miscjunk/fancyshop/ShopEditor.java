package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopEditor implements InventoryHolder {
    Shop shop;
    Inventory viewInv;
    static final int LAST_DEAL=26;
    static final int BUY_SELL=34;
    static final int REMOVE=35;
    ItemStack buyBtn;
    ItemStack sellBtn;
    ItemStack removeBtn;
    ItemStack doneBtn;
    enum State {BUY, SELL, REMOVE}
    State state;
    Map<Integer, Deal> dealMap;

    public ShopEditor(Shop shop) {
        this.shop = shop;
        this.state = State.BUY;
        viewInv = Bukkit.createInventory(this, 36, "Manage Shop");
        buyBtn = new ItemStack(Material.WOOL, 1, (short)5); //green
        sellBtn = new ItemStack(Material.WOOL, 1, (short)11); //blue
        removeBtn = new ItemStack(Material.FIRE, 1);
        doneBtn = new ItemStack(Material.WOOL, 1, (short)5);

        ItemMeta meta = buyBtn.getItemMeta();
        meta.setDisplayName("Editing buy prices");
        List<String> lore = new ArrayList<String>();
        lore.add("Click to edit sell prices");
        meta.setLore(lore);
        buyBtn.setItemMeta(meta);

        meta = sellBtn.getItemMeta();
        meta.setDisplayName("Editing sell prices");
        lore.clear();
        lore.add("Click to edit buy prices");
        meta.setLore(lore);
        sellBtn.setItemMeta(meta);

        meta = removeBtn.getItemMeta();
        meta.setDisplayName("Remove");
        lore.clear();
        lore.add("Click to remove deals");
        meta.setLore(lore);
        removeBtn.setItemMeta(meta);

        meta = doneBtn.getItemMeta();
        meta.setDisplayName("Done");
        doneBtn.setItemMeta(meta);

        changeState(State.BUY);
    }

    private void refreshView(State st) {
        viewInv.clear();
        dealMap = new HashMap<Integer, Deal>();
        for (int i=0; i < shop.deals.size() && i <= LAST_DEAL; i++) {
            Deal d = shop.deals.get(i);
            ItemStack it = d.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            if (st == State.REMOVE) {
                List<String> lore = new ArrayList<String>();
                lore.add(""+ChatColor.RESET+ChatColor.RED+"Click to remove");
                meta.setLore(lore);
            } else {
                meta.setLore(d.toLore());
            }
            it.setItemMeta(meta);
            viewInv.setItem(i, it);
            dealMap.put(i, d);
        }
    }
    private void changeState(State next) {
        refreshView(next);
        switch (next) {
            case BUY:
                viewInv.setItem(BUY_SELL, buyBtn);
                viewInv.setItem(REMOVE, removeBtn);
                break;
            case SELL:
                viewInv.setItem(BUY_SELL, sellBtn);
                viewInv.setItem(REMOVE, removeBtn);
                break;
            case REMOVE:
                viewInv.setItem(BUY_SELL, null);
                viewInv.setItem(REMOVE, doneBtn);
                break;
            default:
                throw new RuntimeException("Unhandled state");
        }
        state = next;
    }

    public Inventory getInventory() {
        return viewInv;
    }

    private void removeDeal(int slot) {
        Deal d = dealMap.get(slot);
        if (d != null) {
            shop.deals.remove(d);
            shop.refreshView();
            viewInv.setItem(slot, null);
        }
    }


    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= 0 && event.getRawSlot() < LAST_DEAL) {
            // click in shop
            switch (event.getAction()) {
                case PICKUP_ALL:
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PICKUP_SOME:
                    if (state == State.REMOVE) removeDeal(event.getRawSlot());
                    event.setCancelled(true);
                    break;
                case SWAP_WITH_CURSOR:
                    ItemStack cursor = event.getCursor();
                    if (state != State.REMOVE) {
                        Deal d = dealMap.get(event.getRawSlot());
                        if (d != null && d.getItem().isSimilar(cursor)) {
                            editDealAmount(d, cursor);
                        } else if (d != null && state == State.BUY) {
                            editBuyPrice(d, cursor);
                        } else if (d != null && state == State.SELL) {
                            editSellPrice(d, cursor);
                        }
                    }
                    event.setCancelled(true);
                    break;
                default:
                    event.setCancelled(true);
            }
        } else if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
            // click in button row
            if (event.getRawSlot() == BUY_SELL) {
                switch (state) {
                    case BUY:
                        changeState(State.SELL);
                        break;
                    case SELL:
                        changeState(State.BUY);
                        break;
                    default:
                }
            } else if (event.getRawSlot() == REMOVE) {
                switch (state) {
                    case REMOVE:
                        changeState(State.BUY);
                        break;
                    default:
                        changeState(State.REMOVE);
                }
            } else {
                // empty slot
            }
            event.setCancelled(true);
        } else {
            // click outside shop
            switch (event.getAction()) {
                case COLLECT_TO_CURSOR:
                case MOVE_TO_OTHER_INVENTORY:
                    event.setCancelled(true);
                    break;
                default:
            }
        }
    }

    private void editBuyPrice(Deal deal, ItemStack item) {
        if (Util.isCurrency(item)) {
            deal.setBuyPrice(item.clone());
            shop.refreshView();
            changeState(state);
        }
    }

    private void editSellPrice(Deal deal, ItemStack item) {
        if (Util.isCurrency(item)) {
            deal.setSellPrice(item.clone());
            shop.refreshView();
            changeState(state);
        }
    }

    private void editDealAmount(Deal deal, ItemStack item) {
        deal.getItem().setAmount(item.getAmount());
        shop.refreshView();
        changeState(state);
    }

    public void onInventoryDrag(InventoryDragEvent event) {
        boolean allow = true;
        for (Integer i : event.getRawSlots()) {
            if (i >= 0 && i < event.getInventory().getSize()) {
                allow = false;
                break;
            }
        }
        event.setCancelled(!allow);
    }
}
