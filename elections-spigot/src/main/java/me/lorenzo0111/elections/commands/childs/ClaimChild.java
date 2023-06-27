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
import me.lorenzo0111.elections.api.objects.DBClaim;
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

import org.bukkit.entity.Player;

public class ClaimChild extends SubCommand {
    private final ElectionsPlus plugin;
    //private final ClaimListener listener;
    
    public ClaimChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
        new ClaimListener(plugin);
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Permission("elections.create")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
        GriefPrevention gp = plugin.getGriefPrevention();
        if (gp == null) {
            Messages.send(sender.audience(), true, "claim", "gp-disabled");
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

            String name = a.get(0);
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
            if (claim == null) {
                Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "create-no-claim-here");
                return;
            }

            plugin.getManager().createClaim(name, claim)
                .thenAccept((eclaim) -> {
                    if (eclaim == null) {
                        Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "create-fail");
                    } else {
                        Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "created");
                    }
                });

            return;
        }

        if (args[1].equalsIgnoreCase("delete")) {
            ArrayList<String> a = plugin.unquote(args, 2);
            if (a.size() != 1) {
                Messages.send(sender.audience(), true, "errors", "bad-args");
                return;
            }

            String name = a.get(0);
            plugin.getManager().getClaimByName(name)
                .thenAccept((claim) -> {
                    if (claim == null) {
                        Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "not-found");
                        return;
                    }

                    plugin.getManager().deleteClaim(claim)
                        .thenAccept((success) -> {
                            if (success) {
                                Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "deleted");
                            } else {
                                Messages.send(sender.audience(), true, Messages.single("name", name), "claim", "delete-fail");
                            }
                        });
                });
        }

        if (args[1].equalsIgnoreCase("list")) {
            plugin.getManager().getClaims()
                .thenAccept((claims) -> {
                    plugin.getLogger().severe(String.format("GOT %d CLAIMS", claims.size()));
                    Map <String, String> placeholders = new HashMap<String, String>();
                    for (DBClaim claim : claims.values()) {
                        placeholders.put("name", claim.getName());
                        placeholders.put("id", claim.getId().toString());
                        placeholders.put("owner", claim.getOwner() == null ? "admin" : claim.getOwner().toString());

                        Messages.send(sender.audience(), true, placeholders, "claim", "list");
                    }
                });

            return;
        }

        Messages.send(sender.audience(), true, "errors", "bad-args");
    }
}

