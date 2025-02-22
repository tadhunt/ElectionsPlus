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
import me.lorenzo0111.elections.conversation.ConversationUtil;
import me.lorenzo0111.elections.conversation.conversations.NameConversation;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.menus.CreateElectionMenu;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.entity.Player;

public class CreateChild extends SubCommand {
    private final ElectionsPlus plugin;

    public CreateChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Permission("elections.create")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
        if (!(sender.player() instanceof Player)) {
            Messages.send(sender.audience(), true, "errors", "console");
            return;
        }

        Player player = (Player)sender.player();

        if (args.length == 1) {
            CreateElectionMenu menu = new CreateElectionMenu(plugin, "None", player);
            ConversationUtil.createConversation(plugin, new NameConversation(player, plugin, menu));
            return;
        }

        ArrayList<String> a = plugin.unquote(args, 1);
        if (a.size() != 1) {
            Messages.send(sender.audience(), true, "errors", "bad-args");
            return;
        }

        String name = a.get(0);

        Cache<UUID, Election> elections = plugin.getCache().getElections();
        if (elections.findByName(name) != null) {
            Messages.send(player, true, "errors", "election-exists");
            return;
        }

        Election election = new Election(UUID.randomUUID(), name, null, true, true);

        elections.add(election.getId(), election);
        elections.persist();

        plugin.claimPartiesRefresh();  // persists any mutations
        plugin.holoRefresh();

        Messages.send(player, true, Messages.single("name", name), "election", "created");
    }
}
