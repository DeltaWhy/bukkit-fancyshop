package net.miscjunk.fancyshop;

import org.bukkit.Location;

public class ShopLocation {
    String world;
    int x;
    int y;
    int z;

    public ShopLocation(Location l) {
        world = l.getWorld().getName();
        x = l.getBlockX();
        y = l.getBlockY();
        z = l.getBlockZ();
    }

    public ShopLocation(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShopLocation)) return false;
        ShopLocation other = (ShopLocation)o;
        return other.world.equals(world) && other.x == x && other.y == y && other.z == z;
    }

    @Override
    public int hashCode() {
        return (world+"x"+x+"y"+y+"z"+z).hashCode();
    }

    @Override
    public String toString() {
        return ("block:"+world+"x"+x+"y"+y+"z"+z);
    }
}
