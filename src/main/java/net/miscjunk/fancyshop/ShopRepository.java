package net.miscjunk.fancyshop;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class ShopRepository {
    private static Plugin plugin;
    private static Database db;

    public static void init(Plugin plugin) {
        if (ShopRepository.plugin != null || ShopRepository.db != null) {
            throw new RuntimeException("Already initialized");
        }
        ShopRepository.plugin = plugin;
        db = new SQLite(Logger.getLogger("Minecraft"), "[FancyShop] ",
                plugin.getDataFolder().getAbsolutePath(), "shops");
        try {
            db.open();
            updateSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't initialize database", e);
        }
    }

    public static void cleanup() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    public static void updateSchema() throws SQLException {
        ResultSet rs = db.query("PRAGMA user_version");
        if (rs.next()) {
            int version = rs.getInt(1);
            if (version == 0) {
                db.query("CREATE TABLE shops ("+
                        "location TEXT NOT NULL,"+
                        "owner TEXT NOT NULL,"+
                        "PRIMARY KEY (location)"+
                        ")");
                db.query("CREATE TABLE deals ("+
                        "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                        "shop_id INT NOT NULL,"+
                        "item TEXT NOT NULL,"+
                        "buy_price TEXT,"+
                        "sell_price TEXT,"+
                        "FOREIGN KEY (shop_id) REFERENCES shops(location)"+
                        ")");
                db.query("PRAGMA user_version=1");
            } else if (version > 1) {
                throw new RuntimeException("Database is newer than plugin version");
            }
        } else {
            throw new RuntimeException("Couldn't get database schema version");
        }
    }

    public static boolean store(Shop shop) {
        try {
            PreparedStatement stmt = db.prepare("INSERT OR REPLACE INTO shops VALUES (?, ?)");
            stmt.setString(1, shop.getLocation().toString());
            stmt.setString(2, shop.getOwner());
            stmt.execute();
            stmt = db.prepare("DELETE FROM deals WHERE shop_id=?");
            stmt.setString(1, shop.getLocation().toString());
            stmt.execute();
            for (Deal d : shop.deals) {
                storeDeal(shop, d);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void storeDeal(Shop shop, Deal deal) throws SQLException {
        PreparedStatement stmt = db.prepare("INSERT INTO deals (shop_id, item, buy_price, sell_price) VALUES (?,?,?,?)");
        stmt.setString(1, shop.getLocation().toString());
        stmt.setString(2, Util.itemToString(deal.getItem()));
        stmt.setString(3, Util.itemToString(deal.getBuyPrice()));
        stmt.setString(4, Util.itemToString(deal.getSellPrice()));
        stmt.execute();
    }

    public static boolean remove(Shop shop) {
        try {
            PreparedStatement stmt = db.prepare("DELETE FROM deals WHERE shop_id=?");
            stmt.setString(1, shop.getLocation().toString());
            stmt.execute();
            stmt = db.prepare("DELETE FROM shops WHERE location=?");
            stmt.setString(1, shop.getLocation().toString());
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Shop load(ShopLocation location, Inventory inv) {
        try {
            PreparedStatement stmt = db.prepare("SELECT * FROM shops WHERE location=?");
            stmt.setString(1, location.toString());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;
            String owner = rs.getString("owner");
            Shop shop = new Shop(location, inv, owner);
            stmt = db.prepare("SELECT * FROM deals WHERE shop_id=?");
            stmt.setString(1, location.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                ItemStack item = Util.stringToItem(rs.getString("item"));
                ItemStack buyPrice = Util.stringToItem(rs.getString("buy_price"));
                ItemStack sellPrice = Util.stringToItem(rs.getString("sell_price"));
                Deal d = new Deal(item, buyPrice, sellPrice);
                shop.deals.add(d);
            }
            shop.refreshView();
            return shop;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}