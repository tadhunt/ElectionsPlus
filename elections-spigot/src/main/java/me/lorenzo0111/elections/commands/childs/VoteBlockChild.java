/*
 * This file is part of ElectionsPlus, licensed under the MIT License.
 *
 * Copyright (c) Lorenzo0111
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.lorenzo0111.elections.commands.childs;

import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.menus.ElectionsMenu;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class VoteBlockChild extends SubCommand implements Listener {
    private final ElectionsPlus plugin;
    private List<Block> blocks;
    
    public VoteBlockChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
        this.blocks = new ArrayList<Block>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setup();
    }

    private void setup() {
        Cache<UUID, ElectionBlock> cacheBlocks = plugin.getCache().getBlocks();

        for (ElectionBlock electionBlock : cacheBlocks.map().values()) {
            Map<String, Object> rawLocation = electionBlock.getLocation();
            Location location = Location.deserialize(rawLocation);

            World world = Bukkit.getWorld("world");
            Block block = world.getBlockAt(location);
            String blockData = block.getBlockData().getAsString();

            if (electionBlock.getBlockData().equals(blockData)) {
                blocks.add(block);
            }
        }
    }

    @Override
    public String getName() {
        return "vote-block";
    }

    @Permission("elections.create")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
        if (args.length != 2) {
            Messages.send(sender.audience(), true, "errors", "bad-args");
            return;
        }
        if (!(sender.player() instanceof Player)) {
            Messages.send(sender.audience(), true, "errors", "console");
            return;
        }

        Player player = (Player)sender.player();
        Block block = player.getTargetBlock(null, 50);

        if (block == null) {
            Messages.send(sender.audience(), true, "errors", "bad-args");
            return;
        }
        if (block.isEmpty()) {
            Messages.send(sender.audience(), true, "errors", "no-block");
            return;
        }

        if (args[1].equalsIgnoreCase("create")) {
            this.create(sender, block);
            return;
        }

        if (args[1].equalsIgnoreCase("delete")) {
            this.delete(sender, block);
            return;
        }

        Messages.send(sender.audience(), true, "errors", "bad-args");
    }

    private void create(User<?>sender, Block block) {
        UUID world = block.getWorld().getUID();
        Location location = block.getLocation();
        String blockData = block.getBlockData().getAsString();

        Cache <UUID, ElectionBlock> eblocks = plugin.getCache().getBlocks();

        if (findElectionBlock(eblocks, world, location) != null) {
            Messages.send(sender.audience(), true, "errors", "block-already-exists");
            return;
        }

        ElectionBlock eblock = new ElectionBlock(UUID.randomUUID(), world, location.serialize(), blockData, true);
        eblocks.add(eblock.getId(), eblock);
        eblocks.persist();

        this.blocks.add(block);

        Messages.send(sender.audience(), true, "vote-block", "created");
    }

    private ElectionBlock findElectionBlock(Cache<UUID, ElectionBlock> eblocks, UUID world, Location location) {
        for(ElectionBlock cblock : eblocks.map().values()) {
            Location cloc = Location.deserialize(cblock.getLocation());

            if (cblock.getWorld().equals(world) && cloc.equals(location)) {
                return cblock;
            }
        }

        return null;
    }

    private void delete(User<?>sender, Block block) {
        UUID world = block.getWorld().getUID();
        Location location = block.getLocation();

        Cache <UUID, ElectionBlock> eblocks = plugin.getCache().getBlocks();
        ElectionBlock eblock = findElectionBlock(eblocks, world, location);

        if (eblock == null) {
            Messages.send(sender.audience(), true, "vote-block", "not-found");
        }

        eblocks.remove(eblock.getId());
        eblocks.persist();

        this.blocks.remove(block);

        Messages.send(sender.audience(), true, "vote-block", "deleted");
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

