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
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class VoteBlockChild extends SubCommand {
    private final ElectionsPlus plugin;
    
    public VoteBlockChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
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

        plugin.getVoteBlockListener().addBlock(block);

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

        plugin.getVoteBlockListener().removeBlock(block);

        Messages.send(sender.audience(), true, "vote-block", "deleted");
    }
}

