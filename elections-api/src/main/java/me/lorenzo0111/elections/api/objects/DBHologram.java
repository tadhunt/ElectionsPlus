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

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.EDatabaseSerializable;

public class DBHologram implements EDatabaseSerializable, ICacheEntry {
    private UUID id;
    private String name;
    private String location;
    private List<String> contents;
    private boolean dirty;

    public DBHologram(UUID id, String name, String location, List<String> contents, boolean dirty) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.contents = contents;
        this.dirty = dirty;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
        this.dirty = true;
    }

    public void setContent(String line) {
        this.contents = new ArrayList<String>();
        this.contents.add(line);
        this.dirty = true;
    }

    public void addContent(String line) {
        this.contents.add(line);
        this.dirty = true;
    }

    public void clear() {
        this.contents.clear();
        this.dirty = true;
    }

    @Override
    public boolean dirty() {
        return dirty;
    }

    @Override
    public void clean() {
        dirty = false;
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        return Getters.database().deleteHologram(this);
    }

    @Override
    public CompletableFuture<Boolean> update() {
        return Getters.database().updateHologram(this);
    }

    public static DBHologram fromResultSet(ResultSet result) throws SQLException {
        UUID id = UUID.fromString(result.getString("id"));
        String name = result.getString("name");
        String location = result.getString("location");
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> contents = new Gson().fromJson(result.getString("contents"), type);

        return new DBHologram(id, name, location, contents, false);
    }

    @Override
    public @NotNull String tableName() {
        return "holograms";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("name", name);
        map.put("location", location);
        map.put("contents", new Gson().toJson(contents));

        return map;
    }
}