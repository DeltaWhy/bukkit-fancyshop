package net.miscjunk.fancyshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
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
import java.util.Set;

public class FancyShopCommandExecutor implements CommandExecutor {
    enum PendingCommand { CREATE, REMOVE, ADMIN_ON, ADMIN_OFF };
    private FancyShop plugin;
    boolean flagsInstalled = false;
    Map<String,PendingCommand> pending;
    Map<String,BukkitTask> tasks;

    public FancyShopCommandExecutor(FancyShop plugin) {
        this.plugin = plugin;
        this.pending = new HashMap<String, PendingCommand>();
        this.tasks = new HashMap<String, BukkitTask>();
        flagsInstalled = Bukkit.getServer().getPluginManager().isPluginEnabled("Flags");
        if (flagsInstalled) {
            Bukkit.getLogger().info("Found Flags, enabling region support");
            io.github.alshain01.flags.Registrar flagsRegistrar = io.github.alshain01.flags.Flags.getRegistrar();
            io.github.alshain01.flags.Flag flag = flagsRegistrar.register("FancyShop", "Allow creating shops", false, "FancyShop", "Entering shops area", "Leaving shops area");
        } else {
            Bukkit.getLogger().info("Flags is not installed, disabling region support");
        }
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
        } else if (args[0].equals("setadmin")) {
            setAdmin(p, cmd, label, args);
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
            case ADMIN_ON:
                if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof InventoryHolder) {
                    event.setCancelled(true);
                    setAdmin(event.getPlayer(), ((InventoryHolder)event.getClickedBlock().getState()).getInventory(), true);
                }
                break;
            case ADMIN_OFF:
                if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof InventoryHolder) {
                    event.setCancelled(true);
                    setAdmin(event.getPlayer(), ((InventoryHolder)event.getClickedBlock().getState()).getInventory(), false);
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
        } else if (!regionAllows(player, inv)) {
            Chat.e(player, "You can't create shops here.");
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

    private void setAdmin(Player player, Inventory inv, boolean admin) {
        if (!Shop.isShop(inv)) {
            Chat.e(player, "That's not a shop!");
        } else {
            Shop shop = Shop.fromInventory(inv, player.getName());
            shop.setAdmin(admin);
            ShopRepository.store(shop);
            if (admin) {
                Chat.s(player, "Set to admin shop.");
            } else {
                Chat.s(player, "Set to normal shop.");
            }
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

    private void setAdmin(Player player, Command cmd, String label, String[] args) {
        if (!player.hasPermission("fancyshop.setadmin")) {
            Chat.e(player, "You don't have permission!");
            return;
        }
        if (args.length == 1 || args.length == 2 && args[1].equals("true")) {
            Chat.i(player, "Right-click a shop to make it an admin shop.");
            setPending(player, PendingCommand.ADMIN_ON);
        } else if (args.length == 2 && args[1].equals("false")) {
            Chat.i(player, "Right-click a shop to make it a normal shop.");
            setPending(player, PendingCommand.ADMIN_OFF);
        } else {
            Chat.e(player, "Usage: /fancyshop setadmin true|false");
        }
    }

    private boolean regionAllows(Player player, Inventory inv) {
        if (!flagsInstalled) return true;
        if (player.hasPermission("fancyshop.create.anywhere")) return true;
        io.github.alshain01.flags.Registrar flagsRegistrar = io.github.alshain01.flags.Flags.getRegistrar();
        io.github.alshain01.flags.Flag flag = flagsRegistrar.getFlag("FancyShop");
        InventoryHolder h = inv.getHolder();
        Location l;
        io.github.alshain01.flags.area.Area area;
        if (h instanceof BlockState) {
            l = ((BlockState)h).getLocation();
            area = io.github.alshain01.flags.CuboidType.getActive().getAreaAt(l);
        } else if (h instanceof DoubleChest) {
            l = ((DoubleChest)h).getLocation();
            area = io.github.alshain01.flags.CuboidType.getActive().getAreaAt(l);
        } else {
            area = io.github.alshain01.flags.CuboidType.DEFAULT.getAreaAt(player.getLocation());
        }
        Set<String> trusted = area.getPlayerTrustList(flag);
        if (trusted.contains(player.getName().toLowerCase())) {
            return true;
        } else {
            for (String s : area.getPermissionTrustList(flag)) {
                if (player.hasPermission(s)) return true;
            }
            return (trusted.isEmpty() && area.getValue(flag, false));
        }
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
        if (sender instanceof Player && ((Player)sender).hasPermission("fancyshop.setadmin")) {
            Chat.i(sender, "    /fancyshop setadmin true - Make a shop an admin shop.\n"+
                    "    /fancyshop setadmin false - Make a shop a normal shop.");
        }
    }
}
