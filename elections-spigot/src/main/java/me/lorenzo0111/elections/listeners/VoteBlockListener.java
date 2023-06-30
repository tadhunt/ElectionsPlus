package me.lorenzo0111.elections.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.menus.ElectionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class VoteBlockListener implements Listener {
    private ElectionsPlus plugin;
    private List<Block> blocks;

    public VoteBlockListener(ElectionsPlus plugin) {
        this.plugin = plugin;
        this.blocks = new ArrayList<>();

        Cache<UUID, ElectionBlock> cacheBlocks = plugin.getCache().getBlocks();

        World world = Bukkit.getWorld("world");
        for (ElectionBlock electionBlock : cacheBlocks.map().values()) {
            Location location = Location.deserialize(electionBlock.getLocation());
            Block block = world.getBlockAt(location);
            String blockData = block.getBlockData().getAsString();

            if (electionBlock.getBlockData().equals(blockData)) {
                blocks.add(block);
            }
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    public void removeBlock(Block block) {
        this.blocks.remove(block);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        //Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();

        if(hand != EquipmentSlot.HAND) {
            return;
        }

        if (!this.blocks.contains(clickedBlock)) {
            return;
        }

        Cache<UUID, Election> elections = plugin.getCache().getElections();
        new ElectionsMenu(player, elections, ElectionsPlus.getInstance()).setup();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block brokenBlock = e.getBlock();

        if (this.blocks.contains(brokenBlock)) {
            e.setCancelled(true);
        }
    }
}
