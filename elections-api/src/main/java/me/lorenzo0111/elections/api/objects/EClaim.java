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
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.EDatabaseSerializable;
import me.lorenzo0111.elections.database.Version;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class EClaim implements EDatabaseSerializable, ICacheEntry {
    private UUID id;
    private String name;
    private UUID owner;
    private Claim claim;
    private Version version;

    public EClaim(UUID id, String name, Claim claim, UUID owner, boolean dirty) {
        this.id = id;
        this.name = name;
        this.claim = claim;
        this.owner = owner;
        this.version = new Version(dirty);
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public Long getGpId() {
        return claim.getID();
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.version.dirty();
    }

    public static EClaim fromResultSet(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        String name = resultSet.getString("name");

        Long gpid = Long.parseLong(resultSet.getString("gpid"));
        UUID owner = null;
        String ownerString = resultSet.getString("owner");
        if (!ownerString.equals("admin")) {
            owner = UUID.fromString(ownerString);
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaim(gpid);
        if (claim == null) {
            return null;
        }

        return new EClaim(id, name, claim, owner, false);
    }

    @Override
    public @NotNull String tableName() {
        return "claims";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id.toString());
        map.put("name", name);
        map.put("gpid", claim.getID().toString());
        if (owner == null) {
            map.put("owner", "admin");
        } else {
            map.put("owner", owner.toString());
        }

        return map;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        return Getters.database().deleteClaim(this);
    }

    @Override
    public CompletableFuture<Boolean> update() {
        return Getters.database().updateClaim(this);
    }
}