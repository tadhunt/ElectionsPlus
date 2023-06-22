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

import me.lorenzo0111.elections.ElectionsHologram;
import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.Command;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class HoloChild extends SubCommand implements Listener {
    private final ElectionsPlus plugin;
    
    public HoloChild(Command command, ElectionsPlus plugin) {
        super(command);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "holo";
    }

    @Permission("elections.create")
    @Override
    public void handleSubcommand(User<?> sender, String[] args) {
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
            if (a.size() != 2) {
                Messages.send(sender.audience(), true, "errors", "bad-args");
                return;
            }

            Location location = player.getLocation();
            String name = a.remove(0);

            ElectionsHologram hologram = plugin.holoCreate(name, location, a);
            if (hologram == null) {
                Messages.send(sender.audience(), true, "hologram", "create-fail");
                return;
            }

            Messages.send(sender.audience(), true, Messages.single("name", name), "hologram", "created");
            return;
        }

        if (args[1].equalsIgnoreCase("delete")) {
            ArrayList<String> a = plugin.unquote(args, 2);
            if (a.size() != 1) {
                Messages.send(sender.audience(), true, "errors", "bad-args");
                return;
            }

            String name = a.get(0);

            Boolean success = plugin.holoDelete(name);
            if (success) {
                Messages.send(sender.audience(), true, Messages.single("name", name), "hologram", "deleted");
                return;
            }

            Messages.send(sender.audience(), true, Messages.single("name", name), "hologram", "not-found");
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            Collection<ElectionsHologram> holograms = plugin.holoList();

            for (ElectionsHologram holo : holograms) {
                Messages.send(sender.audience(), true, Messages.multiple("name", holo.getName(), "location", holo.getLocation().toString()), "hologram", "list");
            }

            return;
        }

        if (args[1].equalsIgnoreCase("refresh")) {
            plugin.holoRefresh();
            Messages.send(sender.audience(), true, "hologram", "refreshed");
            return;
        }

        Messages.send(sender.audience(), true, "errors", "bad-args");
    }
}

