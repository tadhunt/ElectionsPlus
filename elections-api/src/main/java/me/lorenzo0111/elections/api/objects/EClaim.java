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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class EClaim implements DatabaseSerializable {
    private String name;
    private Claim claim;

    public EClaim(String name, Claim claim) {
        this.claim = claim;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return claim.getID();
    }

    public Claim getClaim() {
        return claim;
    }

    public void delete() {
        Getters.database().deleteClaim(this);
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) {
        String name = (String) keys.get("name");
        Long id = Long.parseLong((String) keys.get("id"));

        Claim claim = GriefPrevention.instance.dataStore.getClaim(id);
        if (claim == null) {
            return null;
        }

        return new EClaim(name, claim);
    }

    @Override
    public @NotNull String tableName() {
        return "claims";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("id", claim.getID().toString());

        return map;
    }

    public String toJson() {
        Map<String, Object> m = this.serialize();

        return new Gson().toJson(m);
    }
}