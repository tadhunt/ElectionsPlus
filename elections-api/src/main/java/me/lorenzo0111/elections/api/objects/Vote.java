/*
 * This file is part of ElectionsPlus, licensed under the MIT License.
 *
 * Copyright (c) Lorenzo0111, tadhunt
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

import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.pluginslib.database.DatabaseSerializable;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Vote implements DatabaseSerializable, ICacheEntry {
    private final UUID id;
    private final UUID player;
    private final String party;
    private final UUID electionId;
    private boolean dirty;

    public Vote(UUID id, UUID player, String party, UUID electionId, boolean dirty) {
        this.dirty = dirty;
        this.id = id;
        this.player = player;
        this.party = party;
        this.electionId = electionId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return null;
    }

    public String getParty() {
        return party;
    }

    public UUID getElectionId() {
        return electionId;
    }

    public UUID getPlayer() {
        return player;
    }

    @Override
    public DatabaseSerializable from(Map<String, Object> keys) {
        UUID id = UUID.fromString((String) keys.get("id"));
        UUID player = UUID.fromString((String) keys.get("player"));
        String party = (String) keys.get("party");
        UUID electionId = UUID.fromString((String) keys.get("electionId"));

        return new Vote(id, player, party, electionId, false);
    }

    public static Vote fromResultSet(ResultSet results) {
        try {
            UUID voteId = UUID.fromString(results.getString("id"));
            UUID player = UUID.fromString(results.getString("player"));
            String partyName = results.getString("party");
            UUID electionId = UUID.fromString(results.getString("electionId"));

            return new Vote(voteId, player, partyName, electionId, false);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public @NotNull String tableName() {
        return "votes";
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("player", player);
        map.put("party", party);
        map.put("electionId", electionId);

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Vote vote = (Vote) o;
        return Objects.equals(id, vote.id) && Objects.equals(player, vote.player) && Objects.equals(party, vote.party) && Objects.equals(electionId, vote.electionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, player, party, electionId);
    }

    public boolean dirty() {
        return dirty;
    }

    public void delete() {
        Getters.database().deleteVote(this);
    }

    public void update() throws RuntimeException {
        if (!dirty) {
            return;
        }
        throw new RuntimeException("votes cannot be changed");
    }
}
