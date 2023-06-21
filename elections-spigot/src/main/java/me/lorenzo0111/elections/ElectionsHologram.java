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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings.Visibility;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.handlers.Messages;

public class ElectionsHologram {
    private ElectionsPlus plugin;
    private HolographicDisplaysAPI holoApi;
    private Hologram hologram;
    private String name;
    private Location location;
    private ArrayList<String> contents;

    ElectionsHologram(ElectionsPlus plugin, HolographicDisplaysAPI api, String name, Location location) {
        this.plugin = plugin;
        this.holoApi = api;
        this.name = name;
        this.location = location;
        this.contents = new ArrayList<String>();

        this.hologram = holoApi.createHologram(this.location);

        VisibilitySettings v = this.hologram.getVisibilitySettings();

        this.plugin.getLogger().warning("ElectionsHologram: visibility: " + v.toString());
        
        v.clearIndividualVisibilities();
        v.setGlobalVisibility(Visibility.VISIBLE);
    }

    public String name() {
        return name;
    }

    public Location location() {
        return location;
    }

    public void set(List<String> newContents) {
        contents = new ArrayList<String>(newContents);
        refresh();
    }
            
    public void set(String content) {
            contents.clear();
            contents.add(content);

            refresh();
    }

    public void refresh() {
        plugin.getLogger().warning("refresh starting");
        this.plugin.getElectionsStatus().thenAccept((electionsStatus) -> {
                plugin.getLogger().warning("refresh: got elections status");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        HologramLines holoLines = hologram.getLines();
                        holoLines.clear();

                        plugin.getLogger().warning(String.format("refresh: contents size %d", contents.size()));

                        for (String line : contents) {
                            plugin.getLogger().warning("LINE: " + line);
                            if (!line.equals("%elections_status%")) {
                                plugin.getLogger().warning("use as-is");
                                holoLines.appendText(line);
                                //holoLines.appendText(Messages.componentString(false, Messages.single("text", line), "hologram", "text"));
                                continue;
                            }

                            plugin.getLogger().warning("replace with elections status");

                            if (electionsStatus.size() == 0) {
                                plugin.getLogger().warning("DOAH: no electionsstatus");
                            }

                            for (ElectionStatus status : electionsStatus.values()) {
                                plugin.getLogger().warning("election status: " + status.toString());

                                Election election = status.getElection();
                                Map<String, String> placeholders = Messages.multiple("name", election.getName(), "totalvotes", status.totalVotes().toString());
                                if (election.isOpen()) {
                                    holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "open"));
                                    continue;
                                }

                                Map<String, Integer> winners = status.winners();
                                if (winners == null || winners.size() == 0) {
                                    holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "closed-no-winner"));
                                    continue;
                                }

                                String s = "";
                                for (String partyName : winners.keySet()) {
                                    if (!s.equals("")) {
                                        s += ", ";
                                    }
                                    s += partyName;
                                }

                                if (winners.size() == 1) {
                                    placeholders.put("party", s);
                                    holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "closed-winner"));
                                    continue;
                                }

                                placeholders.put("parties", s);
                                holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "closed-tie"));
                            }
                        }
                    } catch(Exception e) {
                        this.plugin.getLogger().severe("EXCEPTION: " + e.toString());
                    }
                });
        });
    }

    public void clear() {
        hologram.getLines().clear();
    }

    public void delete() {
        hologram.delete();
    }
}
