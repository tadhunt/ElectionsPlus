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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.lorenzo0111.elections.api.objects.DBHologram;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.handlers.Messages;

public class ElectionsHologram {
    private ElectionsPlus plugin;
    private HolographicDisplaysAPI holoApi;
    private Hologram holo;
    private DBHologram dbholo;

    ElectionsHologram(ElectionsPlus plugin, HolographicDisplaysAPI api, String name, Location location, List<String> contents, Boolean persist) {
        if (contents == null) {
            contents = new ArrayList<String>();
        }

        this.plugin = plugin;
        this.holoApi = api;
        this.dbholo = new DBHologram(name, new Gson().toJson(location.serialize()), contents, persist);
        this.holo = holoApi.createHologram(location);

        refresh();
    }

    ElectionsHologram(ElectionsPlus plugin, HolographicDisplaysAPI api, DBHologram dbholo) {
        this.plugin = plugin;
        this.holoApi = api;
        this.dbholo = dbholo;
        this.holo = holoApi.createHologram(getLocation());

        refresh();
    }

    public String getName() {
        return dbholo.getName();
    }

    public Location getLocation() {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();

        return Location.deserialize(new Gson().fromJson(dbholo.getLocation(), type));
    }

    public void refresh() {
        Map<UUID, ElectionStatus> statuses = this.plugin.getElectionStatuses();
        this.update(statuses);
    }

    private void update(Map<UUID, ElectionStatus> statuses) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                HologramLines holoLines = holo.getLines();
                holoLines.clear();

                for (String line : dbholo.getContents()) {
                    if (!line.equals("%elections_status%")) {
                        holoLines.appendText(Messages.componentString(false, Messages.single("text", line), "hologram", "text"));
                        continue;
                    }

                    for (ElectionStatus status : statuses.values()) {
                        Election election = status.getElection();
                        Map<String, String> placeholders = Messages.multiple("name", election.getName(), "totalvotes", status.getTotalVotes().toString());
                        if (election.isOpen()) {
                            placeholders.put("state", Messages.get("open"));
                            holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "open"));
                            continue;
                        }

                        placeholders.put("state", Messages.get("closed"));

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
                        placeholders.put("winner", s);

                        if (winners.size() == 1) {
                            holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "closed-winner"));
                            continue;
                        }

                        holoLines.appendText(Messages.componentString(false, placeholders, "hologram-status", "closed-tie"));
                    }
                }
            } catch(Exception e) {
                this.plugin.getLogger().severe("EXCEPTION: " + e.toString());
            }
        });
    }

    public void clear() {
        holo.getLines().clear();
        dbholo.clear();
    }

    public void delete() {
        holo.delete();
        dbholo.delete();
    }
}
