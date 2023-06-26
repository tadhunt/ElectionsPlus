package me.lorenzo0111.elections.listeners;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;

public class ClaimListener {
    private ElectionsPlus plugin;
    private Logger logger;

    ClaimListener(ElectionsPlus plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @EventHandler
    public void onClaimPermission(ClaimPermissionCheckEvent event) {
        Player player = event.getCheckedPlayer();
        Claim claim = event.getClaim();
        plugin.getManager().getClaimById(claim.getID())
            .thenAccept((eclaim) -> {
                if (eclaim == null) {
                    return;
                }
                if (claim.isAdminClaim()) {
                }
            });
    }

    @EventHandler
    public void onClaimDeleted(ClaimPermissionCheckEvent event) {
    }
}