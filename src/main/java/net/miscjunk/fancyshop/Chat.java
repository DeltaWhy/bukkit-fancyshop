package net.miscjunk.fancyshop;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Chat {
    public static void e(CommandSender s, String message) {
        s.sendMessage(ChatColor.RED+message);
    }
    public static void i(CommandSender s, String message) {
        s.sendMessage(ChatColor.GOLD+message);
    }
    public static void s(CommandSender s, String message) {
        s.sendMessage(ChatColor.GREEN+message);
    }
}
