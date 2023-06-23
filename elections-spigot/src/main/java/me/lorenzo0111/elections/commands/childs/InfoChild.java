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

import me.lorenzo0111.elections.ElectionStatus;
import me.lorenzo0111.elections.ElectionsPlus;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.pluginslib.audience.User;
import me.lorenzo0111.pluginslib.command.ICommand;
import me.lorenzo0111.pluginslib.command.SubCommand;
import me.lorenzo0111.pluginslib.command.annotations.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfoChild extends SubCommand {

    public InfoChild(ICommand<?> command) {
        super(command);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Permission("elections.info")
    @Override
    public void handleSubcommand(User<?> user, String[] args) {
        ElectionsPlus plugin = (ElectionsPlus) getCommand().getPlugin();

        if (args.length < 2) {
            user.audience().sendMessage(Messages.component(true, "errors", "election-name-missing"));
            return;
        }

        ArrayList<String> a = plugin.unquote(args, 1);
        if (a.size() != 1) {
            user.audience().sendMessage(Messages.component(true, "errors", "bad-args"));
            return;
        }
        String electionName = a.get(0);

        ElectionStatus status = plugin.getElectionStatus(electionName);
        if (status == null) {
            user.audience().sendMessage(Messages.component(true, Messages.single("name", electionName), "errors", "election-not-found"));
            return;
        }

        Map<String, String> calcPlaceholders = Messages.single("election", electionName);
        if (status.getElection().isOpen()) {
            calcPlaceholders.put("state", Messages.get("open"));
        } else {
            calcPlaceholders.put("state", Messages.get("closed"));
        }

        user.audience().sendMessage(Messages.component(true, calcPlaceholders, "votes", "title1"));
        user.audience().sendMessage(Messages.component(true, "votes", "title2"));

        Map<String, Integer> votes = status.getPartyVotes();
        if (votes.size() == 0) {
            user.audience().sendMessage(Messages.component(true, "votes", "status-no-parties"));
            return;
        }

        Map<String, Integer> winners = status.winners();
        String winText = "";
        switch (winners.size()) {
        case 0:
            break;
        case 1:
            winText = Messages.get("winner");
            break;
        default:
            winText = Messages.get("tie");
            break;
        }

        Integer total = status.getTotalVotes();
        for(String partyName : votes.keySet()) {
            Integer nvotes = votes.get(partyName);
            Integer percent = nvotes * 100 / total;

            HashMap<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("party", partyName);
            placeholders.put("nvotes", nvotes.toString());
            placeholders.put("percent", percent.toString());

            if (status.getElection().isOpen()) {
                placeholders.put("state", Messages.get("open"));

                user.audience().sendMessage(Messages.component(true, placeholders, "votes", "status-open"));
                continue;
            }

            placeholders.put("state", Messages.get("closed"));

            if (winners.get(partyName) == null) {
                user.audience().sendMessage(Messages.component(true, placeholders, "votes", "status-closed"));
            } else {
                placeholders.put("winner", winText);
                user.audience().sendMessage(Messages.component(true, placeholders, "votes", "status-closed-winner"));
            }
        }
    }
}
