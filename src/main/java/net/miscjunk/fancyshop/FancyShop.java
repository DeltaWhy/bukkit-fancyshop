package net.miscjunk.fancyshop;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class FancyShop extends JavaPlugin implements Listener {
    FancyShopCommandExecutor cmdExecutor;
    public void onDisable() {
        ShopRepository.cleanup();
    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        cmdExecutor = new FancyShopCommandExecutor(this);
        getCommand("fancyshop").setExecutor(cmdExecutor);
        ShopRepository.init(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!canBeShop(event.getClickedBlock()) || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Inventory inv = ((InventoryHolder)event.getClickedBlock().getState()).getInventory();
        if (event.getPlayer().isSneaking()) {
            if (Shop.isShop(inv)) {
                Shop shop = Shop.fromInventory(inv, event.getPlayer().getName());
                if (!shop.getOwner().equals(event.getPlayer().getName()) && !event.getPlayer().hasPermission("fancyshop.open")) {
                    event.setCancelled(true);
                    Chat.e(event.getPlayer(), "You don't have permission to open this shop chest.");
                }
            }
        } else {
            Player p = event.getPlayer();
            if (cmdExecutor.hasPending(p)) {
                cmdExecutor.onPlayerInteract(event);
            } else {
                if (Shop.isShop(inv)) {
                    event.setCancelled(true);
                    if (p.hasPermission("fancyshop.use")) {
                        Shop shop = Shop.fromInventory(inv, p.getName());
                        if (p.getName().equals(shop.getOwner()) && event.getMaterial() != Material.STICK) {
                            shop.edit(p);
                        } else {
                            shop.open(p);
                        }
                    } else {
                        Chat.e(p, "You don't have permission!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Shop) {
            ((Shop)event.getInventory().getHolder()).onInventoryClick(event);
        } else if (event.getInventory().getHolder() instanceof ShopEditor) {
            ((ShopEditor)event.getInventory().getHolder()).onInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Shop) {
            ((Shop)event.getInventory().getHolder()).onInventoryDrag(event);
        } else if (event.getInventory().getHolder() instanceof ShopEditor) {
            ((ShopEditor)event.getInventory().getHolder()).onInventoryDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (Shop.isShop(event.getInventory())) {
            // update shop view after using chest
            Shop shop = Shop.fromInventory(event.getInventory(), "");
            shop.refreshView();
            shop.refreshEditor();
        } else if (event.getInventory().getHolder() instanceof ShopEditor) {
            // save shop after editing
            Shop shop = ((ShopEditor)event.getInventory().getHolder()).getShop();
            ShopRepository.store(shop);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canBeShop(event.getBlock())) return;
        Inventory inv = ((InventoryHolder)event.getBlock().getState()).getInventory();
        if (Shop.isShop(inv)) {
            Chat.e(event.getPlayer(), "You can't break a shop chest. First remove the shop with /fancyshop remove.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        for (int i=0; i < event.blockList().size(); i++) {
            Block b = event.blockList().get(i);
            if (!canBeShop(b)) continue;
            Inventory inv = ((InventoryHolder)b.getState()).getInventory();
            if (Shop.isShop(inv)) {
                event.blockList().remove(i);
                i--;
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!canBeShop(event.getBlock())) return;
        Inventory inv = ((InventoryHolder)event.getBlock().getState()).getInventory();
        if (Shop.isShop(inv)) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;
        Block above = event.getBlock().getRelative(BlockFace.UP);
        if (!canBeShop(above)) return;
        Inventory inv = ((InventoryHolder)above.getState()).getInventory();
        if (!Shop.isShop(inv)) return;
        Shop shop = Shop.fromInventory(inv, "");
        if (shop.getOwner().equals(event.getPlayer().getName())) return; // we'll assume they know what they're doing
        event.setCancelled(true);
        Chat.e(event.getPlayer(), "You can't place that here.");
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!Shop.isShop(event.getSource())) return;
        if (event.getInitiator().getHolder() instanceof Hopper) return; // allow hoppers to work because only the owner can place them
        event.setCancelled(true);
    }

    private boolean canBeShop(Block block) {
        if (block == null) return false;
        return block.getState() instanceof InventoryHolder;
    }
}

