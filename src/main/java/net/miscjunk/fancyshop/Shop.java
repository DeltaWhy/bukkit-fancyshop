package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
import java.util.UUID;

public class Shop implements InventoryHolder {
    ShopLocation location;
    Inventory sourceInv;
    Inventory viewInv;
    UUID owner;
    String name;
    boolean admin;
    List<Deal> deals;
    Map<Integer, Deal> dealMap;
    ShopEditor editor;

    static Map<ShopLocation, Shop> shopMap;

    public Shop(ShopLocation location, Inventory inv, UUID owner, String name, boolean admin) {
        this.location = location;
        this.owner = owner;
        this.name = name;
        this.admin = admin;
        sourceInv = inv;
        viewInv = Bukkit.createInventory(this, 27, name);
        deals = new ArrayList<Deal>();
        refreshView();
    }

    public static Shop fromInventory(Inventory inv, UUID owner) {
        if (shopMap == null) shopMap = new HashMap<ShopLocation, Shop>();
        InventoryHolder h = inv.getHolder();
        Location l;
        if (h instanceof BlockState) {
            l = ((BlockState)h).getLocation();
        } else if (h instanceof DoubleChest) {
            l = ((DoubleChest)h).getLocation();
        } else {
            return null;
        }
        ShopLocation loc = new ShopLocation(l);
        if (shopMap.containsKey(loc) && shopMap.get(loc) != null) {
            return shopMap.get(loc);
        } else {
            Shop shop = ShopRepository.load(loc, inv);
            String name = I18n.s("shop.default-name", Bukkit.getServer().getOfflinePlayer(owner).getName());
            if (shop == null) shop = new Shop(loc, inv, owner, name, false);
            shopMap.put(loc, shop);
            return shop;
        }
    }

    public static Shop fromInventory(Inventory inv) {
        if (shopMap == null) shopMap = new HashMap<ShopLocation, Shop>();
        InventoryHolder h = inv.getHolder();
        Location l;
        if (h instanceof BlockState) {
            l = ((BlockState)h).getLocation();
        } else if (h instanceof DoubleChest) {
            l = ((DoubleChest)h).getLocation();
        } else {
            return null;
        }
        ShopLocation loc = new ShopLocation(l);
        if (shopMap.containsKey(loc) && shopMap.get(loc) != null) {
            return shopMap.get(loc);
        } else {
            return null;
        }
    }

    public static boolean isShop(Inventory inv) {
        if (shopMap == null) shopMap = new HashMap<ShopLocation, Shop>();
        InventoryHolder h = inv.getHolder();
        Location l;
        if (h instanceof BlockState) {
            l = ((BlockState)h).getLocation();
        } else if (h instanceof DoubleChest) {
            l = ((DoubleChest)h).getLocation();
        } else {
            return false;
        }
        ShopLocation loc = new ShopLocation(l);
        if (!shopMap.containsKey(loc)) shopMap.put(loc, ShopRepository.load(loc, inv));
        return (shopMap.get(loc) != null);
    }

    public static void removeShop(ShopLocation loc) {
        if (shopMap != null && shopMap.containsKey(loc)) shopMap.remove(loc);
    }

    public void open(Player player) {
        player.openInventory(viewInv);
    }

    public void edit(Player player) {
        if (editor == null) editor = new ShopEditor(this);
        player.openInventory(editor.viewInv);
    }

    public Inventory getInventory() {
        return viewInv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        viewInv = Bukkit.createInventory(this, 27, name);
        refreshView();
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
        refreshEditor();
        refreshView();
    }

    public void setLocation(ShopLocation location) {
        this.location = location;
    }

    public ShopLocation getLocation() {
        return location;
    }

    public void refreshEditor() {
        if (editor != null) editor.refreshView();
    }

    public void refreshView() {
        dealMap = new HashMap<Integer, Deal>();
        refreshDeals();
        viewInv.clear();
        int i = 0;
        for (Deal deal : deals) {
            if (deal.getBuyPrice() == null && deal.getSellPrice() == null) continue;
            List<String> lore = deal.toLore(admin);
            ItemStack view = deal.getItem().clone();
            ItemMeta meta = view.getItemMeta();
            meta.setLore(lore);
            view.setItemMeta(meta);
            dealMap.put(i, deal);
            viewInv.setItem(i, view);
            i++;
        }
    }

    private void refreshDeals() {
        for (Deal deal : deals) {
            if (deal.getBuyPrice() != null) {
                deal.setAvailable(countItems(sourceInv, deal.getItem()));
            } else {
                deal.setAvailable(0);
            }
            if (deal.getSellPrice() != null) {
                int currency = countItems(sourceInv, deal.getSellPrice());
                deal.setBuying(deal.getItem().getAmount() * currency/deal.getSellPrice().getAmount());
            } else {
                deal.setBuying(0);
            }
            int i = -1;
            for (Map.Entry<Integer,Deal> e : dealMap.entrySet()) {
                if (e.getValue() == deal) {
                    i = e.getKey();
                    break;
                }
            }
            if (i != -1) {
                ItemStack view = viewInv.getItem(i);
                ItemMeta meta = view.getItemMeta();
                meta.setLore(deal.toLore(admin));
                view.setItemMeta(meta);
            }
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
        if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
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
                    }
                    break;
                default:
                    event.setCancelled(true);
            }
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

    private void sellAll(HumanEntity whoClicked, Deal deal, InventoryView view) {
        ItemStack cursor = view.getCursor();
        while (deal.getItem().isSimilar(cursor) && deal.getItem().getAmount() <= cursor.getAmount()) {
            if (!sell(whoClicked, deal, view)) break;
        }
    }


    private boolean sell(HumanEntity whoClicked, Deal deal, InventoryView view) {
        if (!(whoClicked instanceof Player)) return false;
        Player p = (Player)whoClicked;
        ItemStack cursor = view.getCursor();
        if (deal.getItem().isSimilar(cursor)) {
            if (deal.getItem().getAmount() > cursor.getAmount()) {
                Chat.e(p, I18n.s("sell.amount"));
                return false;
            } else {
                if (!admin && !sourceInv.containsAtLeast(deal.getSellPrice(), deal.getSellPrice().getAmount())) {
                    Chat.e(p, I18n.s("sell.stock"));
                    return false;
                } else {
                    // try depositing item
                    Map<Integer, ItemStack> overflow;
                    if (!admin) {
                        overflow = sourceInv.addItem(deal.getItem().clone());
                        if (!overflow.isEmpty()) {
                            Chat.e(p, I18n.s("sell.room"));
                            int numOverflowed = 0;
                            for (ItemStack it : overflow.values()) {
                                if (it.isSimilar(deal.getItem())) numOverflowed += it.getAmount();
                            }
                            ItemStack toRemove = deal.getItem().clone();
                            toRemove.setAmount(deal.getItem().getAmount() - numOverflowed);
                            sourceInv.removeItem(toRemove);
                            return false;
                        }
                        sourceInv.removeItem(deal.getSellPrice());
                    }
                    cursor.setAmount(cursor.getAmount()-deal.getItem().getAmount());
                    if (cursor.getAmount() == 0) view.setCursor(null);
                    overflow = whoClicked.getInventory().addItem(deal.getSellPrice().clone());
                    for (ItemStack it : overflow.values()) {
                        if (cursor == null || cursor.getAmount() == 0) {
                            cursor = it;
                            view.setCursor(it);
                        } else {
                            whoClicked.getWorld().dropItemNaturally(whoClicked.getLocation(), it);
                        }
                    }
                    refreshDeals();
                    return true;
                }
            }
        } else {
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
        if (!(whoClicked instanceof Player)) return false;
        Player p = (Player)whoClicked;
        ItemStack cursor = view.getCursor();
        if (deal.getBuyPrice().isSimilar(cursor)) {
            if (deal.getBuyPrice().getAmount() > cursor.getAmount()) {
                Chat.e(p, I18n.s("buy.enough"));
                return false;
            } else {
                if (!admin && !sourceInv.containsAtLeast(deal.getItem(), deal.getItem().getAmount())) {
                    Chat.e(p, I18n.s("buy.stock"));
                    return false;
                } else {
                    // try depositing currency
                    Map<Integer, ItemStack> overflow;
                    if (!admin) {
                        overflow = sourceInv.addItem(deal.getBuyPrice().clone());
                        if (!overflow.isEmpty()) {
                            Chat.e(p, I18n.s("buy.room"));
                            int numOverflowed = 0;
                            for (ItemStack it : overflow.values()) {
                                if (it.isSimilar(deal.getBuyPrice())) numOverflowed += it.getAmount();
                            }
                            ItemStack toRemove = deal.getBuyPrice().clone();
                            toRemove.setAmount(deal.getBuyPrice().getAmount() - numOverflowed);
                            sourceInv.removeItem(toRemove);
                            return false;
                        }
                        sourceInv.removeItem(deal.getItem());
                    }
                    cursor.setAmount(cursor.getAmount()-deal.getBuyPrice().getAmount());
                    if (cursor.getAmount() == 0) {
                        view.setCursor(deal.getItem().clone());
                    } else {
                        overflow = whoClicked.getInventory().addItem(deal.getItem().clone());
                        for (ItemStack it : overflow.values()) {
                            whoClicked.getWorld().dropItemNaturally(whoClicked.getLocation(), it);
                        }
                    }
                    refreshDeals();
                    return true;
                }
            }
        } else {
            return false;
        }
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
