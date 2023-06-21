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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;

public class DBHologram implements DatabaseSerializable {
    private String name;
    private String location;
    private List<String> contents;

    public DBHologram(String name, String location, List<String> contents) {
        init(name, location, contents, false);
    }

    public DBHologram(String name, String location, List<String> contents, Boolean persist) {
        init(name, location, contents, persist);
    }

    private void init(String name, String location, List<String> contents, Boolean persist) {
        this.name = name;
        this.location = location;
        this.contents = contents;

        if (persist) {
            persist();
        }
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
        persist();
    }

    public void setContent(String line) {
        this.contents = new ArrayList<String>();
        this.contents.add(line);
        persist();
    }

    public void addContent(String line) {
        this.contents.add(line);
        persist();
    }

    public void clear() {
        this.contents.clear();
        persist();
    }

    public void delete() {
        Getters.database().deleteHologram(this.getName());
    }

    private void persist() {
        Getters.database().updateHologram(this);
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) {
        String jsonString = (String) keys.get("json");

        return new Gson().fromJson(jsonString, getClass());
    }

    @Override
    public @NotNull String tableName() {
        return "holograms";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String,Object> map = new HashMap<>();

        map.put("name", name);
        map.put("json", new Gson().toJson(this));

        return map;
    }
}