package me.lorenzo0111.elections;

import org.bukkit.Location;

public interface IElectionsHologram {
    public String getName();
    public Location getLocation();
    public void refresh();
    public void clear();
    public void delete();
}