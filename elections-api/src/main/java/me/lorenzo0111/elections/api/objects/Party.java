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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Party implements DatabaseSerializable, ICacheEntry {
    private final UUID id;
    private final String name;
    private String icon;
    private UUID owner;
    private final List<UUID> members;
    private boolean dirty;

    public Party(UUID id, String name, UUID owner, String icon, List<UUID> members, boolean dirty) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.icon = icon;
        this.members = members;
        this.dirty = dirty;
    }

    public Party(UUID id, String name, UUID owner, boolean dirty) {
        this(id, name, owner, "", new ArrayList<>(), dirty);
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public void addMember(UUID uuid) {
        if (this.members.contains(uuid)) {
            return;
        }

        this.members.add(uuid);
        dirty = true;
    }

    public void removeMember(UUID uuid) {
        if (this.members.remove(uuid)) {
            dirty = true;
        }
    }

    public List<UUID> getMembers() {
        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.members.remove(owner);
        this.dirty = true;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        this.dirty = true;
    }

    public String getIcon() {
        if (icon.equals("")) {
            return null;
        }
        return icon;
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) {
        UUID id = UUID.fromString((String) keys.get("id"));
        String name = (String) keys.get("name");
        UUID owner = UUID.fromString((String) keys.get("owner"));
        String icon = (String) keys.get("icon");

        Type type = new TypeToken<ArrayList<UUID>>() {}.getType();
        List<UUID> members = new Gson().fromJson((String) keys.get("members"), type);

        return new Party(id, name, owner, icon, members, false);
    }

    public static Party fromResultSet(ResultSet resultSet) {
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            String name = resultSet.getString("name");
            UUID owner = UUID.fromString(resultSet.getString("owner"));
            String icon = resultSet.getString("icon");

            Type type = new TypeToken<ArrayList<UUID>>() {}.getType();
            List<UUID> members = new Gson().fromJson(resultSet.getString("members"), type);
    
            return new Party(id, name, owner, icon, members, false);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public @NotNull String tableName() {
        return "parties";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", name);
        map.put("name", name);
        map.put("owner", owner);
        map.put("icon", icon);
        map.put("members", new Gson().toJson(members));

        return map;
    }

    public boolean dirty() {
        return dirty;
    }

    public void delete() {
        Getters.database().deleteParty(this);
    }

    public void update() {
        Getters.database().updateParty(this);
    }

}
