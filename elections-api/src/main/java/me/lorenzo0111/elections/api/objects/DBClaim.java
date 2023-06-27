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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;

public class DBClaim implements DatabaseSerializable {
    private String name;
    private Long id;
    private UUID owner;

    public DBClaim(String name, Long id) {
        this.name = name;
        this.id = id;
        this.owner = null;
    }

    public DBClaim(String name, Long id, UUID owner) {
        this.name = name;
        this.id = id;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        Getters.database().updateClaim(this);
    }

    public void delete() {
        Getters.database().deleteClaim(this);
    }

    public static DBClaim fromResultSet(ResultSet resultSet) {
        try {
            String name = resultSet.getString("name");
            Long id = Long.parseLong(resultSet.getString("id"));
            UUID owner = null;
            String ownerString = resultSet.getString("owner");
            if (!ownerString.equals("admin")) {
                owner = UUID.fromString(ownerString);
            }

            return new DBClaim(name, id, owner);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) {
        String name = (String) keys.get("name");
        Long id = Long.parseLong((String) keys.get("id"));

        UUID owner = null;
        String ownerString = (String) keys.get("owner");
        if (!ownerString.equals("admin")) {
            owner = UUID.fromString(ownerString);
        }

        return new DBClaim(name, id, owner);
    }

    @Override
    public @NotNull String tableName() {
        return "claims";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("id", id.toString());
        if (owner == null) {
            map.put("owner", "admin");
        } else {
            map.put("owner", owner.toString());
        }

        return map;
    }

    public String toJson() {
        Map<String, Object> m = this.serialize();

        return new Gson().toJson(m);
    }
}