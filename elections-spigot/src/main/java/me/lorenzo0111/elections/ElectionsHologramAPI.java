package me.lorenzo0111.elections;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.google.gson.Gson;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.DBHologram;

public class ElectionsHologramAPI {
    private final ElectionsPlus plugin;
    private HolographicDisplaysAPI holographicDisplaysApi = null;
    private boolean decentHologramsApi = false;

    public ElectionsHologramAPI(ElectionsPlus plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            this.holographicDisplaysApi = HolographicDisplaysAPI.get(plugin);
            plugin.getLogger().info("HolographicDisplays Holograms enabled");
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            this.decentHologramsApi = true;
            plugin.getLogger().info("DecentHolograms Holograms enabled");
        }

        plugin.getLogger().info("Holograms disabled: add HolograpicDisplays plugin to enable.");
    }

    public boolean enabled() {
        if (holographicDisplaysApi != null) {
            return true;
        }

        if (decentHologramsApi == true) {
            return true;
        }

        return false;
    }

    IElectionsHologram create(DBHologram dbholo) {
        if(this.holographicDisplaysApi != null) {
            return new ElectionsHDHologram(this.plugin, this.holographicDisplaysApi, dbholo);
        }
        if(this.decentHologramsApi) {
            return new ElectionsDHHologram(this.plugin, dbholo);
        }

        return null;
    }

    IElectionsHologram create(String name, Location location, List<String> contents, boolean dirty) {
        if (contents == null) {
            contents = new ArrayList<String>();
        }

        Cache<UUID, DBHologram> dbholos = plugin.getCache().getHolograms();
        DBHologram dbholo = new DBHologram(UUID.randomUUID(), name, new Gson().toJson(location.serialize()), contents, dirty);
        dbholos.add(dbholo.getId(), dbholo);
        dbholos.persist();

        return this.create(dbholo);
    }
}