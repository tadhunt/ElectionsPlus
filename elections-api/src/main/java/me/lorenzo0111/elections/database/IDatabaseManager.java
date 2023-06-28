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

package me.lorenzo0111.elections.database;

import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.DBHologram;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.api.objects.Vote;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.ryanhamshire.GriefPrevention.Claim;

public interface IDatabaseManager {
    void closeConnection() throws SQLException;

    //CompletableFuture<Election> createElection(String name, Map<String, Party> parties);
    CompletableFuture<Map<String, Election>> getElections();
    CompletableFuture<Boolean> updateElection(Election election);
    CompletableFuture<Boolean> deleteElection(Election election);

    CompletableFuture<Map<String, Party>> getParties();
    CompletableFuture<Boolean> updateParty(Party party);
    CompletableFuture<Boolean> deleteParty(Party party);

    CompletableFuture<List<Vote>> getVotes();
    CompletableFuture<?> updateVote(Vote vote);
    CompletableFuture<?> deleteVote(Vote vote);

    CompletableFuture<List<ElectionBlock>> getBlocks();
    CompletableFuture<Boolean> updateBlock(ElectionBlock electionBlock);
    CompletableFuture<Boolean> deleteBlock(ElectionBlock electionBlock);

    CompletableFuture<Map<String, DBHologram>> getHolograms();
    CompletableFuture<Boolean> updateHologram(DBHologram hologram);
    CompletableFuture<Boolean> deleteHologram(DBHologram hologram);

    CompletableFuture<Map<String, EClaim>> getClaims();
    CompletableFuture<Boolean> updateClaim(EClaim claim);
    CompletableFuture<Boolean> deleteClaim(EClaim claim);
}
