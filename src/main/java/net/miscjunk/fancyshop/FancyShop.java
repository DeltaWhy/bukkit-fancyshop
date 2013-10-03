package net.miscjunk.fancyshop;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class FancyShop extends JavaPlugin implements Listener {
    public void onDisable() {
        ShopRepository.cleanup();
    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        ShopRepository.init(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.STICK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
                event.setCancelled(true);
                Shop shop = Shop.fromInventory(((InventoryHolder)event.getClickedBlock().getState()).getInventory(),
                        event.getPlayer().getName());
                shop.open(event.getPlayer());
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.PAPER) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
                event.setCancelled(true);
                Shop shop = Shop.fromInventory(((InventoryHolder)event.getClickedBlock().getState()).getInventory(),
                        event.getPlayer().getName());
                shop.edit(event.getPlayer());
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.BOOK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
                Inventory inv = ((InventoryHolder)event.getClickedBlock().getState()).getInventory();
                if (Shop.isShop(inv)) {
                    event.setCancelled(true);
                    Bukkit.broadcastMessage("Saving");
                    ShopRepository.store(Shop.fromInventory(inv, event.getPlayer().getName()));
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
            Shop.fromInventory(event.getInventory(),"").refreshView();
        }
    }
}

