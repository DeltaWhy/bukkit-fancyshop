package net.miscjunk.fancyshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class FancyShopCommandExecutor implements CommandExecutor {
    enum PendingCommand { CREATE, REMOVE };
    private FancyShop plugin;
    Map<String,PendingCommand> pending;
    Map<String,BukkitTask> tasks;

    public FancyShopCommandExecutor(FancyShop plugin) {
        this.plugin = plugin;
        this.pending = new HashMap<String, PendingCommand>();
        this.tasks = new HashMap<String, BukkitTask>();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player.");
            return true;
        }

        Player p = (Player)sender;

        if (args.length < 1) {
            printUsage(sender);
        } else if (args[0].equals("create")) {
            create(p, cmd, label, args);
        } else if (args[0].equals("remove")) {
            remove(p, cmd, label, args);
        } else {
            printUsage(sender);
        }
        return true;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!pending.containsKey(event.getPlayer().getName())) return;
        switch (pending.get(event.getPlayer().getName())) {
            case CREATE:
                if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof InventoryHolder) {
                    event.setCancelled(true);
                    create(event.getPlayer(), ((InventoryHolder)event.getClickedBlock().getState()).getInventory());
                }
                break;
            case REMOVE:
                if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof InventoryHolder) {
                    event.setCancelled(true);
                    remove(event.getPlayer(), ((InventoryHolder) event.getClickedBlock().getState()).getInventory());
                }
                break;
        }
    }

    private void remove(Player player, Inventory inv) {
        if (Shop.isShop(inv)) {
            Shop shop = Shop.fromInventory(inv, player.getName());
            if (!shop.getOwner().equals(player.getName()) && !player.hasPermission("fancyshop.remove")) {
                Chat.e(player, "That's not your shop!");
            } else {
                ShopRepository.remove(shop);
                Shop.removeShop(shop.getLocation());
                Chat.s(player, "Shop removed.");
            }
        } else {
            Chat.e(player, "That's not a shop!");
        }
        clearPending(player);
    }

    private void create(Player player, Inventory inv) {
        if (Shop.isShop(inv)) {
            Chat.e(player, "That's already a shop!");
        } else {
            Shop shop = Shop.fromInventory(inv, player.getName());
            ShopRepository.store(shop);
            Chat.s(player, "Shop created.");
            Chat.i(player, "Right-click your shop with a stick to see it as a customer.");
            Chat.i(player, "Shift-right-click to open the chest to manage inventory.");
            shop.edit(player);
        }
        clearPending(player);
    }

    private void remove(Player player, Command cmd, String label, String[] args) {
        if (!player.hasPermission("fancyshop.create")) { //not typo - can't remove if we can't create
            Chat.e(player, "You don't have permission!");
            return;
        } else if (args.length > 1) {
            Chat.e(player, "Usage: /fancyshop remove");
            return;
        }
        Chat.i(player, "Right-click a shop to remove it.");
        setPending(player, PendingCommand.REMOVE);
    }

    private void create(Player player, Command cmd, String label, String[] args) {
        if (!player.hasPermission("fancyshop.create")) {
            Chat.e(player, "You don't have permission!");
            return;
        } else if (args.length > 1) {
            Chat.e(player, "Usage: /fancyshop create");
            return;
        }
        Chat.i(player, "Right-click a chest to create a shop there.");
        setPending(player, PendingCommand.CREATE);
    }

    private void setPending(Player player, PendingCommand cmd) {
        final String name = player.getName();
        if (tasks.containsKey(name)) {
            BukkitTask task = tasks.get(name);
            if (task != null) task.cancel();
        }
        pending.put(name, cmd);
        tasks.put(name, new BukkitRunnable() {
            public void run() {
                pending.remove(name);
            }
        }.runTaskLater(plugin, 60 * 20));
    }

    private void clearPending(Player player) {
        final String name = player.getName();
        if (tasks.containsKey(name)) {
            BukkitTask task = tasks.get(name);
            if (task != null) task.cancel();
            tasks.remove(name);
        }
        if (pending.containsKey(name)) pending.remove(name);
    }

    public boolean hasPending(Player player) {
        return pending.containsKey(player.getName());
    }

    public void printUsage(CommandSender sender) {
        Chat.i(sender, "/fancyshop: Create and manage shops.\n"+
                "    /fancyshop create - Create a new shop.\n"+
                "    /fancyshop remove - Remove a shop.");
    }
}
