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

import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.Vote;
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
            Election election = plugin.getCache().getElections().get(name);

            if (election == null)
                return PlaceholderAPIPlugin.booleanFalse();

            return election.isOpen() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }

        if (params.startsWith("voted_")) {
            String name = params.split("voted_")[1];
            Vote vote = plugin.getCache().getVotes().get(name+"||"+player.getUniqueId());

            return vote != null ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();

        }

        return null;
    }
}
