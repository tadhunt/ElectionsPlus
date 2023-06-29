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

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.EDatabaseSerializable;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
 
public class ElectionBlock implements EDatabaseSerializable, ICacheEntry {
    private final UUID id;
    private final UUID world;
    private final Map<String, Object> location;
    private final String blockData;
    private boolean dirty;

    public ElectionBlock(UUID id, UUID world, Map<String, Object> location, String blockData, boolean dirty) {
        this.id = id;
        this.world = world;
        this.location = location;
        this.blockData = blockData;
        this.dirty = dirty;
    }

    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return id.toString();
    }

    public static ElectionBlock fromResultSet(ResultSet result) throws SQLException {
        UUID id = UUID.fromString(result.getString("id"));
        UUID world = UUID.fromString(result.getString("world"));
        Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
        Map<String, Object> location = new Gson().fromJson(result.getString("location"), type);
        String blockData = result.getString("blockdata");

        return new ElectionBlock(id, world, location, blockData, false);
    }

    @Override
    public @NotNull String tableName() {
        return "blocks";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id.toString());
        map.put("world", world.toString());
        map.put("location", new Gson().toJson(location));
        map.put("blockdata", blockData);

        return map;
    }

    public UUID getWorld() {
        return this.world;
    }

    public Map<String, Object> getLocation() {
        return location;
    }

    public String getBlockData() {
        return blockData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof ElectionBlock)) {
            return false;
        }

        ElectionBlock b = (ElectionBlock)o;

        if (!this.world.equals(b.world)) {
            return false;
        }

        if (!this.location.equals(b.location)) {
            return false;
        }

        if (!this.blockData.equals(b.blockData)) {
            return false;
        }

        return true;
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        return Getters.database().deleteBlock(this);
    }

    @Override
    public CompletableFuture<Boolean> update() {
        return Getters.database().updateBlock(this);
    }

    @Override
    public void clean() {
        dirty = false;
    }

    public boolean dirty() {
        return dirty;
    }
}