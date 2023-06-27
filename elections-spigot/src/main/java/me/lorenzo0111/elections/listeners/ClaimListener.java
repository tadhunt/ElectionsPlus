package me.lorenzo0111.elections.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.lorenzo0111.elections.ElectionsPlus;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;

public class ClaimListener implements Listener {
    private final ElectionsPlus plugin;

    public ClaimListener(ElectionsPlus plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClaimPermission(ClaimPermissionCheckEvent event) {
        Long id = event.getClaim().getID();
        UUID newOwner = event.getCheckedUUID();

        plugin.getLogger().info(String.format("onClaimPermission: claim %d newOwner %s", id, newOwner == null ? "admin" : newOwner.toString()));

        plugin.getManager().getClaimById(id)
            .thenAccept((eclaim) -> {
                if (eclaim == null) {
                    return;
                }
                if (eclaim.getOwner().equals(newOwner)) {
                    return;
                }

                eclaim.setOwner(newOwner);
            });
    }

    @EventHandler
    public void onClaimDeleted(ClaimPermissionCheckEvent event) {
        plugin.getLogger().info("onClaimDeleted: unimplemented");
    }
}