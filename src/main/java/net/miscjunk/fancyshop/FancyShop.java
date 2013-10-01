package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public class FancyShop extends JavaPlugin implements Listener {
    public void onDisable() {
        // TODO: Place any custom disable code here.
    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
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
                        "Herobrine");
                shop.open(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Shop) {
            ((Shop)event.getInventory().getHolder()).onInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Shop) {
            event.setCancelled(true);
        }
    }
}

