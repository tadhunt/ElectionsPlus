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
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.listeners.ClaimListener;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class ClaimsChild extends SubCommand {
    private final ElectionsPlus plugin;
    //private final ClaimListener listener;
    
    public ClaimsChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
        new ClaimListener(plugin);
    }

    @Override
    public String getName() {
        return "claims";
    }

    @Permission("elections.create")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
        GriefPrevention gp = plugin.getGriefPrevention();
        if (gp == null) {
            Messages.send(sender.audience(), true, "claims", "gp-disabled");
            return;
        }

        if (!(sender.player() instanceof Player)) {
            Messages.send(sender.audience(), true, "errors", "console");
            return;
        }
        Player player = (Player)sender.player();

        if (args.length < 2) {
            Messages.send(sender.audience(), true, "errors", "bad-args");
            return;
        }

        if (args[1].equalsIgnoreCase("create")) {
            ArrayList<String> a = plugin.unquote(args, 2);
            if (a.size() != 1) {
                Messages.send(sender.audience(), true, "errors", "bad-args");
                return;
            }

            Cache<UUID, EClaim> claims = plugin.getCache().getClaims();

            String name = a.get(0);

            if (claims.findByName(name) != null) {
                Messages.send(sender.audience(), true, Messages.single("name", name), "claims", "already-exists");
                return;
            }

            Claim gclaim = gp.dataStore.getClaimAt(player.getLocation(), false, null);
            if (gclaim == null) {
                Messages.send(sender.audience(), true, Messages.single("name", name), "claims", "create-no-claim-here");
                return;
            }

            EClaim eclaim = plugin.findClaimByGpId(gclaim.getID());
            if (eclaim != null) {
                Messages.send(sender.audience(), true, Messages.single("name", eclaim.getName()), "claims", "gp-already-exists");
                return;
            }

            eclaim = new EClaim(UUID.randomUUID(), name, gclaim, player.getUniqueId(), true);
            claims.add(eclaim.getId(), eclaim);
            claims.persist();

            plugin.claimPartiesUpdate(eclaim);  // persists any mutations

            Messages.send(sender.audience(), true, Messages.single("name", name), "claims", "created");

            return;
        }

        if (args[1].equalsIgnoreCase("delete")) {
            ArrayList<String> a = plugin.unquote(args, 2);
            if (a.size() != 1) {
                Messages.send(sender.audience(), true, "errors", "bad-args");
                return;
            }
            String name = a.get(0);

            Cache<UUID, EClaim> claims = plugin.getCache().getClaims();
            EClaim eclaim = claims.findByName(name);
            if (eclaim == null) {
                Messages.send(sender.audience(), true, Messages.single("name", name), "claims", "not-found");
                return;
            }
            claims.remove(eclaim.getId());
            claims.persist();
            Messages.send(sender.audience(), true, Messages.single("name", name), "claims", "deleted");

            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            Map <String, String> placeholders = new HashMap<String, String>();
            for (EClaim claim : plugin.getCache().getClaims().map().values()) {
                placeholders.put("name", claim.getName());
                placeholders.put("id", claim.getId().toString());

                UUID owner = claim.getOwner();
                if (owner == null) {
                    placeholders.put("owner", "Admin");
                } else {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(owner);
                    if (p == null) {
                        placeholders.put("owner", owner.toString());
                    } else {
                        placeholders.put("owner", p.getName());
                    }
                }

                Messages.send(sender.audience(), true, placeholders, "claims", "list");
            }

            return;
        }

        Messages.send(sender.audience(), true, "errors", "bad-args");
    }
}

