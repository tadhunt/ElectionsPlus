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
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.conversation.ConversationUtil;
import me.lorenzo0111.elections.conversation.conversations.CreatePartyConversation;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.menus.PartiesMenu;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PartiesChild extends SubCommand {
    private final ElectionsPlus plugin;

    public PartiesChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "parties";
    }

    @Permission(value = "elections.parties")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
        if (!(sender.player() instanceof Player)) {
            Messages.send(sender.audience(), true, "errors", "console");
            return;
        }

        Player player = (Player)sender.player();

        if (args.length < 2) {
            Cache<UUID, Party> parties = plugin.getCache().getParties();
            new PartiesMenu(player, parties, plugin).setup();
            return;
        }

        ArrayList<String> a = plugin.unquote(args, 1);
        String command = a.remove(0);

        if (command.equalsIgnoreCase("create")) {
            CreatePartyConversation conversation = new CreatePartyConversation(player, plugin);
            switch (a.size()) {
            default:
                player.sendMessage(Messages.componentString(true, "errors", "bad-args"));
                break;
            case 0:
                ConversationUtil.createConversation(plugin, conversation);
                break;
            case 1:
                String partyName = a.get(0);
                conversation.handle(partyName);
                break;
            }
            return;
        }
 
        if (command.equalsIgnoreCase("delete")) {
            switch (a.size()) {
            default:
                player.sendMessage(Messages.componentString(true, "errors", "bad-args"));
                break;
            case 1:
                String partyName = a.get(0);
                if (partyName == "") {
                    player.sendMessage(Messages.componentString(true, "errors", "party-name-required"));
                    return;
                }

                Cache<UUID, Party> parties = plugin.getCache().getParties();
                Party party = parties.findByName(partyName);
                if (party == null) {
                    player.sendMessage(Messages.componentString(true, Messages.single("party", partyName), "errors", "party-not-found"));
                    return;
                }

                this.plugin.deleteParty(party);
                player.sendMessage(Messages.componentString(true, "parties", "deleted"));
                break;
            }
            return;
        }

        if (command.equalsIgnoreCase("add-member")) {
            switch (a.size()) {
            default:
                player.sendMessage(Messages.componentString(true, "errors", "bad-args"));
                break;
            case 2:
                String partyName = a.get(0);
                if (partyName.equals("")) {
                    player.sendMessage(Messages.componentString(true, "errors", "party-name-required"));
                    return;
                }
                String memberName = a.get(1);

                if (memberName.equals("")) {
                    player.sendMessage(Messages.componentString(true, "errors", "member-name-required"));
                    return;
                }

                OfflinePlayer member = Bukkit.getPlayer(memberName);
                if (member == null) {
                    player.sendMessage(Messages.componentString(true, Messages.single("name", memberName), "errors", "user-not-online"));
                    return;
                }

                Cache <UUID, Party> parties = plugin.getCache().getParties();
                Party party = parties.findByName(partyName);
                if (party == null) {
                    player.sendMessage(Messages.componentString(true, Messages.single("party", partyName), "errors", "party-not-found"));
                    return;
                }

                party.addMember(member.getUniqueId());
                parties.persist();

                player.sendMessage(Messages.componentString(true, Messages.multiple("name", member.getName(), "party", party.getName()), "parties", "user-added"));
                break;
            }

            return;
        }

        player.sendMessage(Messages.componentString(true, "errors", "command-not-found"));
    }
}
