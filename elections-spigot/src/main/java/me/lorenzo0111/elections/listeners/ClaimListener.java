package me.lorenzo0111.elections.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.Party;
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


        EClaim eclaim = plugin.findClaimByGpId(gclaim.getID());
        if (eclaim != null) {
            plugin.getLogger().info(String.format("onClaimTransfer[claim %d/%s]: newOwner %s", gclaim.getID(), eclaim.getName(), newOwner == null ? "admin" : newOwner.toString()));
            plugin.claimTransfer(gclaim, eclaim, newOwner);
        } else {
            plugin.getLogger().info(String.format("onClaimTransfer[claim %d]: not tracked (ignore)", gclaim.getID()));
        }
    }

    public void onClaimDeleted(ClaimPermissionCheckEvent event) {
        Claim gclaim = event.getClaim();

        plugin.getLogger().info(String.format("onClaimDeleted[claim %d]: claim deleted", gclaim.getID()));

        EClaim eclaim = plugin.findClaimByGpId(gclaim.getID());
        if (eclaim == null) {
            plugin.getLogger().info(String.format("onClaimDeleted[claim %d]: no corresponding elections claim to delete", gclaim.getID()));
            return;
        }

        Cache<UUID, Party> parties = plugin.getCache().getParties();

        Party party = parties.findByName(eclaim.getName());
        if (party != null) {
            plugin.deleteParty(party);
        }

        Cache<UUID, EClaim> eclaims = plugin.getCache().getClaims();

        eclaims.remove(eclaim.getId());
        eclaims.persist();

        plugin.getLogger().info(String.format("onClaimDeleted[claim %d/%s]: claim deleted", gclaim.getID(), eclaim.getName()));
    }
}