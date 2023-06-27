package me.lorenzo0111.elections.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.database.IDatabaseManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;

public class ClaimListener implements Listener {
    private final ElectionsPlus plugin;

    public ClaimListener(ElectionsPlus plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClaimTransfer(ClaimTransferEvent event) {
        Claim gclaim = event.getClaim();
        UUID newOwner = event.getNewOwner();

        plugin.getLogger().info(String.format("onClaimTransfer[claim %d]: newOwner %s", gclaim.getID(), newOwner == null ? "admin" : newOwner.toString()));

        plugin.getManager().getClaimById(gclaim.getID())
            .thenAccept((eclaim) -> {
                plugin.claimTransfer(gclaim, eclaim, newOwner);
            });
    }

    public void onClaimDeleted(ClaimPermissionCheckEvent event) {
        Claim gclaim = event.getClaim();

        plugin.getLogger().info(String.format("onClaimDeleted[claim %d]: claim deleted", gclaim.getID()));

        IDatabaseManager manager = plugin.getManager();
        manager.getClaimById(gclaim.getID())
            .thenAccept((eclaim) -> {
                if (eclaim == null) {
                    plugin.getLogger().info(String.format("onClaimDeleted[claim %d]: no corresponding elections claim to delete", gclaim.getID()));
                    return;
                }
                manager.deleteClaim(eclaim)
                    .thenAccept((success) -> {
                        if (success) {
                            plugin.getLogger().info(String.format("onClaimDeleted[claim %d/%s]: claim deleted", gclaim.getID(), eclaim.getName()));
                        } else {
                            plugin.getLogger().warning(String.format("onClaimDeleted[claim %d/%s]: claim deletion failed", gclaim.getID(), eclaim.getName()));
                        }
                    });
            });
    }
}