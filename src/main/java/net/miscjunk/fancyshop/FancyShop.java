package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.IOException;

public class FancyShop extends JavaPlugin implements Listener {
    FancyShopCommandExecutor cmdExecutor;
    boolean allowExplosion;
    boolean allowBreak;
    boolean allowHoppers;
    boolean allowHoppersIn;

    public void onDisable() {
        ShopRepository.cleanup();
    }

    public void onEnable() {
        this.saveDefaultConfig();
        allowExplosion = this.getConfig().getBoolean("allow-explosion");
        allowBreak = this.getConfig().getBoolean("allow-break");
        allowHoppers = this.getConfig().getBoolean("allow-hoppers");
        allowHoppersIn = this.getConfig().getBoolean("allow-hoppers-in");
        getServer().getPluginManager().registerEvents(this, this);
        cmdExecutor = new FancyShopCommandExecutor(this);
        CurrencyManager.init(this);
        getCommand("fancyshop").setExecutor(cmdExecutor);
        ShopRepository.init(this);
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            Bukkit.getLogger().info("Failed to send metrics");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!canBeShop(event.getClickedBlock()) || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Inventory inv = ((InventoryHolder)event.getClickedBlock().getState()).getInventory();
        if (event.getPlayer().isSneaking()) {
            if (Shop.isShop(inv)) {
                Shop shop = Shop.fromInventory(inv, event.getPlayer().getUniqueId(), event.getPlayer().getName()+"'s Shop");
                if (!shop.getOwner().equals(event.getPlayer().getUniqueId()) && !event.getPlayer().hasPermission("fancyshop.open")) {
                    event.setCancelled(true);
                    Chat.e(event.getPlayer(), "You don't have permission to open this shop chest.");
                }
            }
        } else {
            Player p = event.getPlayer();
            if (cmdExecutor.hasPending(p)) {
                // wait for the high-priority handler to catch it
            } else {
                if (Shop.isShop(inv)) {
                    event.setCancelled(true);
                    if (p.hasPermission("fancyshop.use")) {
                        Shop shop = Shop.fromInventory(inv);
                        if (p.getUniqueId().equals(shop.getOwner()) && event.getMaterial() != Material.STICK) {
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract2(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (event.isCancelled()) return;
        if (!canBeShop(event.getClickedBlock()) || event.getAction() != Action.RIGHT_CLICK_BLOCK || p.isSneaking()) return;
        if (!cmdExecutor.hasPending(p)) return;
        cmdExecutor.onPlayerInteract(event);
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
            Shop shop = Shop.fromInventory(event.getInventory());
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
            Player player = event.getPlayer();
            if (allowBreak) {
                Shop shop = Shop.fromInventory(inv);
                if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("fancyshop.remove")) {
                    Chat.e(player, "You don't have permission to break that.");
                    event.setCancelled(true);
                } else {
                    ShopRepository.remove(shop);
                    Shop.removeShop(shop.getLocation());
                    Chat.s(player, "Shop removed.");
                }
            } else {
                Chat.e(player, "You can't break a shop chest. First remove the shop with /fancyshop remove.");
                event.setCancelled(true);
            }
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
                if (allowExplosion) {
                    Shop shop = Shop.fromInventory(inv);
                    ShopRepository.remove(shop);
                    Shop.removeShop(shop.getLocation());
                } else {
                    event.blockList().remove(i);
                    i--;
                }
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
        if (event.getBlock().getType() == Material.HOPPER) {
            if (allowHoppers) return;
            Block above = event.getBlock().getRelative(BlockFace.UP);
            if (!canBeShop(above)) return;
            Inventory inv = ((InventoryHolder)above.getState()).getInventory();
            if (!Shop.isShop(inv)) return;
            Shop shop = Shop.fromInventory(inv);
            if (shop.getOwner().equals(event.getPlayer().getUniqueId())) return; // we'll assume they know what they're doing
            event.setCancelled(true);
            Chat.e(event.getPlayer(), "You can't place that here.");
        } else if (event.getBlock().getType() == Material.CHEST || event.getBlock().getType() == Material.TRAPPED_CHEST) {
            Inventory inv = ((InventoryHolder)event.getBlock().getState()).getInventory();
            if (!(inv instanceof DoubleChestInventory)) return;
            DoubleChestInventory dc = (DoubleChestInventory)inv;
            Shop shop;
            if (Shop.isShop(dc.getLeftSide())) {
                shop = Shop.fromInventory(dc.getLeftSide());
            } else if (Shop.isShop(dc.getRightSide())) {
                shop = Shop.fromInventory(dc.getRightSide());
            } else {
                return;
            }
            Chat.s(event.getPlayer(), "Extended shop.");
            ShopRepository.remove(shop);
            Shop.removeShop(shop.getLocation());
            Shop.removeShop(new ShopLocation(dc.getHolder().getLocation()));
            shop.setLocation(new ShopLocation(dc.getHolder().getLocation()));
            ShopRepository.store(shop);
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (allowHoppers) return;
        if (allowHoppersIn && event.getInitiator().getHolder() instanceof Hopper) return; // allow hoppers to work because only the owner can place them
        if (!Shop.isShop(event.getSource()) && !Shop.isShop(event.getDestination())) return;
        event.setCancelled(true);
    }

    private boolean canBeShop(Block block) {
        if (block == null) return false;
        return block.getState() instanceof InventoryHolder;
    }
}

