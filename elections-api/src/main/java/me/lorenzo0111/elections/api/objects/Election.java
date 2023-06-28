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
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Election implements DatabaseSerializable, ICacheEntry {
    private final UUID id;
    private final String name;
    private final Map<String, Party> parties;
    private boolean open;
    private boolean dirty;

    public Election(UUID id, String name, Map<String, Party> parties, boolean open, boolean dirty) {
        this.id = id;
        this.name = name;
        this.parties = parties;
        this.open = open;
        this.dirty = dirty;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Party> getParties() {
        return parties;
    }

    public Party getParty(String name) {
        return parties.get(name);
    }

    public Boolean deleteParty(String name) {
        if (parties.remove(name) == null) {
            return false;
        }

        this.dirty = true;

        return true;
    }
            
    public void addParty(Party party) {
        if (parties.get(party.getName()) != null) {
            return;
        }

        parties.put(party.getName(), party);
        this.dirty = true;
    }

    public void close() {
        this.open = false;
        this.dirty = true;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) throws RuntimeException {
        throw new RuntimeException("You can't deserialize this class. You have to do that manually.");
    }

    public static Election fromResultSet(ResultSet result) {
        try {
            String id = result.getString("id");
            String name = result.getString("name");
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> partyStrings = new Gson().fromJson(result.getString("parties"), type);
            Boolean open = result.getInt("open") != 0;

            return new Election(id, name, parties, open, false);

        } catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
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

    public boolean dirty() {
        return dirty;
    }

    public void delete() {
        Getters.database().deleteElection(this);
    }

    public void update() {
        if (!dirty) {
            return;
        }
        Getters.database().updateElection(this);
    }
}