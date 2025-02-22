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

package me.lorenzo0111.elections;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.Vote;

import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ElectionsPlusPlaceholderExpansion extends PlaceholderExpansion {
    private final ElectionsPlus plugin;

    public ElectionsPlusPlaceholderExpansion(ElectionsPlus plugin) {
            this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "elections";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lorenzo0111";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.3";
    }

    /*
        Placeholders:
        %elections_open%
        %election_isopen%
        %elections_isopen_<name>%
        %elections_voted_<election>%
    */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("info")) {
            Map<UUID, Election> elections = plugin.getCache().getElections().map();

            String r = "";
            for (Election election : elections.values()) {
                if (election.isOpen()) {
                    //results.add(election.getName() + ": open");
                    r += election.getName() + ": open";
                } else {
                    r += election.getName() + ": closed";
                }
            }
            return r;
        }

        if (params.equalsIgnoreCase("open")) {
            return String.valueOf(plugin.getCache().getElections().size());
        }

        if (params.equalsIgnoreCase("isopen")) {
            return plugin.getCache()
                    .getElections()
                    .map()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().isOpen())
                    .findFirst()
                    .map((e) -> PlaceholderAPIPlugin.booleanTrue())
                    .orElse(PlaceholderAPIPlugin.booleanFalse());
        }

        if (params.startsWith("isopen_")) {
            String name = params.split("isopen_")[1];
            Cache<UUID, Election> elections = plugin.getCache().getElections();

            for(Election election : elections.map().values()) {
                if (election.getName().equals(name)) {
                    return election.isOpen() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
                }
            }

            return PlaceholderAPIPlugin.booleanFalse();
        }

        if (params.startsWith("voted_")) {
            String electionName = params.split("voted_")[1];

            Cache<UUID, Election> elections = plugin.getCache().getElections();
            Election election = elections.findByName(electionName);

            if (election == null) {
                return PlaceholderAPIPlugin.booleanFalse();
            }

            Cache<UUID, Vote> votes = plugin.getCache().getVotes();

            Vote vote = plugin.findVote(votes, election, player.getUniqueId());
            if (vote == null) {
                return PlaceholderAPIPlugin.booleanFalse();
            }

            return PlaceholderAPIPlugin.booleanTrue();

        }

        return null;
    }
}
