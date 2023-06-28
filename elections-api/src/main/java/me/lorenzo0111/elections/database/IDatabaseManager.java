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

    CompletableFuture<Election> createElection(String name, Map<String, Party> parties);
    CompletableFuture<List<Election>> getElections();
    CompletableFuture<Boolean> updateElection(Election election);
    CompletableFuture<Boolean> deleteElection(Election election);

    CompletableFuture<Map<String, Party>> getParties();
    CompletableFuture<Party> getParty(String name);
    CompletableFuture<Party> createParty(String name, UUID owner);
    CompletableFuture<Boolean> deleteParty(String name);
    CompletableFuture<Boolean> deleteParty(Party party);
    CompletableFuture<Boolean> updateParty(Party party);

    CompletableFuture<List<Vote>> getVotes();
    CompletableFuture<Boolean> vote(UUID player, Party party, Election election);
    CompletableFuture<Boolean> vote(Vote vote);
    CompletableFuture<?> deleteVote(Vote vote);

    CompletableFuture<List<ElectionBlock>> getElectionBlocks();
    CompletableFuture<ElectionBlock> createElectionBlock(UUID world, Map <String, Object> location, String blockData);
    CompletableFuture<Boolean> deleteElectionBlock(ElectionBlock electionBlock);

    CompletableFuture<Map<String, DBHologram>> getHolograms();
    CompletableFuture<DBHologram> createHologram(String name, String location, List<String> contents);
    CompletableFuture<Boolean> deleteHologram(String name);
    CompletableFuture<Boolean> updateHologram(DBHologram hologram);

    CompletableFuture<Map<String, EClaim>> getClaims();
    CompletableFuture<EClaim> getClaimById(Long id);
    CompletableFuture<EClaim> getClaimByName(String name);
    CompletableFuture<EClaim> createClaim(String name, Claim gclaim, UUID owner);
    CompletableFuture<Boolean> deleteClaim(EClaim claim);
    CompletableFuture<Boolean> updateClaim(EClaim claim);
}
