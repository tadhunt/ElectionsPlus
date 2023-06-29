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

package me.lorenzo0111.elections.api.objects;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.EDatabaseSerializable;
import me.lorenzo0111.elections.database.Version;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Election implements EDatabaseSerializable, ICacheEntry {
    private final UUID id;
    private final String name;
    private final Map<UUID, Boolean> parties;
    private boolean open;
    private Version version;

    public Election(UUID id, String name, Set<UUID> parties, boolean open, boolean dirty) {
        Map<UUID, Boolean> pmap = new HashMap<>();
        if (parties != null) {
            for (UUID party : parties) {
                pmap.put(party, true);
            }
        }

        this.id = id;
        this.name = name;
        this.parties = pmap;
        this.open = open;
        this.version = new Version(dirty);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<UUID, Boolean> getParties() {
        return parties;
    }

    public boolean partyExists(UUID id) {
        return parties.get(id);
    }

    public Boolean deleteParty(UUID id) {
        if (parties.remove(id) == null) {
            return false;
        }

        this.version.dirty();

        return true;
    }
            
    public void addParty(UUID id) {
        if (parties.get(id) != null) {
            return;
        }

        parties.put(id, true);
        this.version.dirty();
    }

    public void close() {
        this.open = false;
        this.version.dirty();
    }

    public boolean isOpen() {
        return open;
    }

    public static Election fromResultSet(ResultSet result) throws SQLException {
        UUID id = UUID.fromString(result.getString("id"));
        String name = result.getString("name");

        Type type = new TypeToken<List<UUID>>() {}.getType();
        Set<UUID> parties = new Gson().fromJson(result.getString("parties"), type);

        Boolean open = result.getInt("open") != 0;

        return new Election(id, name, parties, open, false);
    }
        
    @Override
    public @NotNull String tableName() {
        return "elections";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("name", name);
        map.put("parties", new Gson().toJson(parties.keySet()));
        map.put("open", isOpen() ? 1 : 0);

        return map;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        return Getters.database().deleteElection(this);
    }

    @Override
    public CompletableFuture<Boolean> update() {
        return Getters.database().updateElection(this);
    }
}