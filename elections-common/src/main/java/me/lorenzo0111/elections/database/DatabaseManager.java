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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.DBHologram;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.api.objects.Vote;
import me.lorenzo0111.elections.cache.CacheManager;
import me.lorenzo0111.elections.scheduler.IAdvancedScheduler;
import me.lorenzo0111.elections.tasks.CacheTask;
import me.lorenzo0111.pluginslib.database.connection.HikariConnection;
import me.lorenzo0111.pluginslib.database.connection.IConnectionHandler;
import me.lorenzo0111.pluginslib.database.connection.SQLiteConnection;
import me.lorenzo0111.pluginslib.database.objects.Column;
import me.ryanhamshire.GriefPrevention.Claim;

import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DatabaseManager implements IDatabaseManager {
    private Logger logger;
    private ETable votesTable;
    private ETable partiesTable;
    private ETable electionsTable;
    private ETable blocksTable;
    private ETable hologramsTable;
    private ETable claimsTable;

    private final IConnectionHandler connectionHandler;
    private final CacheManager cache;

    public DatabaseManager(Logger logger, IAdvancedScheduler scheduler, CacheManager cache, ConfigurationNode config, IConnectionHandler handler) {
        this.logger = logger;
        this.connectionHandler = handler;

        this.tables(scheduler, cache, config);
        this.cache = cache;
    }

    public DatabaseManager(Logger logger, ConfigurationNode configuration, CacheManager cache, Path directory, IAdvancedScheduler scheduler) throws SQLException {
        this.logger = logger;

        HikariConfig config = new HikariConfig();

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(10);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(5000);

        config.setPoolName("MultiLang MySQL Connection Pool");
        config.setDataSourceClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("serverName", configuration.node("database", "ip").getString());
        config.addDataSourceProperty("port", configuration.node("database", "port").getString());
        config.addDataSourceProperty("databaseName", configuration.node("database", "database").getString());
        config.addDataSourceProperty("user", configuration.node("database", "username").getString());
        config.addDataSourceProperty("password", configuration.node("database", "password").getString());
        config.addDataSourceProperty("useSSL", configuration.node("database", "ssl").getString());

        IConnectionHandler handler = null;

        try {
            handler = new HikariConnection(new HikariDataSource(config));
        } catch (Exception e) {
            try {
                handler = new SQLiteConnection(directory);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        this.connectionHandler = handler;

        this.tables(scheduler, cache, configuration);
        this.cache = cache;
    }

    private void tables(IAdvancedScheduler scheduler, CacheManager cache, ConfigurationNode config) {
        // Votes
        List<Column> votesColumns = new ArrayList<>();
        votesColumns.add(new Column("id", "TEXT"));
        votesColumns.add(new Column("player", "TEXT"));
        votesColumns.add(new Column("party", "TEXT"));
        votesColumns.add(new Column("electionId", "TEXT"));
        this.votesTable = new ETable(logger, scheduler, connectionHandler, "votes", votesColumns);
        this.votesTable.create();

        // Parties
        List<Column> partiesColumns = new ArrayList<>();
        partiesColumns.add(new Column("owner", "TEXT"));
        partiesColumns.add(new Column("name", "TEXT"));
        partiesColumns.add(new Column("members", "TEXT"));
        partiesColumns.add(new Column("icon", "TEXT nullable"));
        this.partiesTable = new ETable(logger, scheduler, connectionHandler, "parties", partiesColumns);
        this.partiesTable.create();
        this.partiesTable.setUnique("idx_parties_name", "name");

        // Elections
        List<Column> electionsColumns = new ArrayList<>();
        electionsColumns.add(new Column("id", "TEXT"));
        electionsColumns.add(new Column("name", "TEXT"));
        electionsColumns.add(new Column("parties", "TEXT"));
        electionsColumns.add(new Column("open", "INTEGER"));
        this.electionsTable = new ETable(logger, scheduler, connectionHandler, "elections", electionsColumns);
        this.electionsTable.create();

        // Blocks
        List<Column> blocksColumns = new ArrayList<>();
        blocksColumns.add(new Column("world", "TEXT"));
        blocksColumns.add(new Column("location", "TEXT"));
        blocksColumns.add(new Column("blockdata", "TEXT"));
        this.blocksTable = new ETable(logger, scheduler, connectionHandler, "blocks", blocksColumns);
        this.blocksTable.create();

        // Holograms
        List<Column> hologramsColumns = new ArrayList<>();
        hologramsColumns.add(new Column("name", "TEXT"));
        hologramsColumns.add(new Column("json", "TEXT"));
        this.hologramsTable = new ETable(logger, scheduler, connectionHandler, "holograms", hologramsColumns);
        this.hologramsTable.create();

        // Claims
        List<Column> claimsColumns = new ArrayList<>();
        claimsColumns.add(new Column("name", "TEXT"));
        claimsColumns.add(new Column("id", "TEXT"));
        claimsColumns.add(new Column("owner", "TEXT"));
        this.claimsTable = new ETable(logger, scheduler, connectionHandler, "claims", claimsColumns);
        this.claimsTable.create();

        scheduler.repeating(new CacheTask(this, cache), 0L, config.node("cache-duration").getInt(5), TimeUnit.MINUTES);
    }

    public ETable getPartiesTable() {
        return partiesTable;
    }

    public ETable getVotesTable() {
        return votesTable;
    }

    public ETable getElectionsTable() {
        return electionsTable;
    }

    public ETable getBlocksTable() {
        return blocksTable;
    }

    public ETable getHologramsTable() {
        return hologramsTable;
    }

    public ETable getClaimsTable() {
        return claimsTable;
    }

    @Override
    public CompletableFuture<Election> createElection(String name, Map<String, Party> parties) {
        CompletableFuture<Election> future = new CompletableFuture<>();

        this.getElectionsTable()
            .find("name", name)
            .thenAccept((result) -> {
                try {
                    if (result.next()) {
                        future.complete(null);
                        return;
                    }

                    Election election = new Election(UUID.randomUUID(), name, parties, true);
                    cache.getElections().add(election.getId(), election);
                    this.getElectionsTable().add(election);
                    future.complete(election);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

        return future;
    }

    @Override
    public void closeConnection() throws SQLException {
        connectionHandler.close();
    }

    @Override
    public CompletableFuture<List<Election>> getElections() {
        CompletableFuture<List<Election>> future = new CompletableFuture<>();

        getParties().
            thenAccept((parties) -> {
                getElectionsTable().run(() -> {
                    try {
                        Statement statement = connectionHandler.getConnection().createStatement();
                        ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s;", getElectionsTable().getName()));

                        List<Election> elections = new ArrayList<>();
                        Gson gson = new Gson();
                        while (resultSet.next()) {
                            UUID id = UUID.fromString(resultSet.getString("id"));
                            String name = resultSet.getString("name");
                            Boolean open = resultSet.getInt("open") != 0;

                            Type type = new TypeToken<ArrayList<String>>() {}.getType();
                            List<String> partyNames = new ArrayList<>(gson.fromJson(resultSet.getString("parties"), type));
                            Map<String, Party> electionParties = new HashMap<String, Party>();
                            for (String partyName : partyNames) {
                                Party party = parties.get(partyName);
                                if (party != null) {
                                    electionParties.put(partyName, party);
                                }
                            }
                            Election election = new Election(id, name, electionParties, open);

                            elections.add(election);
                        }

                        future.complete(elections);
                    } catch (SQLException e) {
                        logger.severe("SQL EXCEPTION: " + e.toString());
                        e.printStackTrace();
                        future.complete(null);
                    } catch (Exception e) {
                        logger.severe("EXCEPTION: " + e.toString());
                        future.complete(null);
                    }
                });
        });

        return future;
    }

    @Override
    public CompletableFuture<Map<String, Party>> getParties() {
        CompletableFuture<Map<String, Party>> future = new CompletableFuture<>();

        getPartiesTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s;", getPartiesTable().getName()));

                Map<String, Party> parties = new HashMap<String, Party>();
                while (resultSet.next()) {
                    Party party = decodeParty(resultSet);
                    parties.put(party.getName(), party);
                }

                future.complete(parties);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }
                    
    private Party decodeParty(ResultSet resultSet) throws SQLException {
        Type type = new TypeToken<ArrayList<UUID>>() {}.getType();
        Gson gson = new Gson();
        List<UUID> members = new ArrayList<>(gson.fromJson(resultSet.getString("members"), type));
        Party party = new Party(resultSet.getString("name"), UUID.fromString(resultSet.getString("owner")), members);

        if (resultSet.getString("icon") != null) {
            party.setIcon(resultSet.getString("icon"));
        }

        return party;
    }

    @Override
    public CompletableFuture<Party> getParty(String partyName) {
        CompletableFuture<Party> future = new CompletableFuture<>();

        getPartiesTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s WHERE name = '%s';", getPartiesTable().getName(), partyName));

                Integer nParties = 0;
                Party party = null;
                while (resultSet.next()) {
                    nParties++;
                    party = decodeParty(resultSet);
                }

                if (nParties > 1) {
                    logger.severe(String.format("getParty: found %d parties with name '%s', expected 0..1", partyName, nParties));
                    future.complete(null);
                }

                future.complete(party);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Party> createParty(String name, UUID owner) {
        CompletableFuture<Party> partyFuture = new CompletableFuture<>();

        partiesTable.find("name", name)
                .thenAccept((it) -> {
                    try {
                        if (it.next()) {
                            partyFuture.complete(null);
                            return;
                        }

                        Party party = new Party(name, owner);
                        partiesTable.add(party);
                        cache.getParties().add(party.getName(), party);
                        partyFuture.complete(party);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        return partyFuture;
    }

    @Override
    public CompletableFuture<Boolean> deleteParty(String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        cache.getParties().remove(name);
        partiesTable.removeWhere("name", name)
            .thenAccept((result) -> {
                if (result instanceof String) {
                    logger.severe(String.format("deleteParty[%s]: %s", name, (String)result));
                    future.complete(false);
                    return;
                }

                // remove this party from all elections
                getElections()
                    .thenAccept((elections) -> {
                        for (Election election : elections) {
                            election.deleteParty(name);
                        }
                        future.complete(true);
                    });
            });

            return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteParty(Party party) {
        return deleteParty(party.getName());
    }

    @Override
    public CompletableFuture<Boolean> updateParty(Party party) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        cache.getParties().remove(party.getName());
        cache.getParties().add(party.getName(), party);
        partiesTable.removeWhere("name", party)
            .thenRun(() -> {
                partiesTable.add(party);
                future.complete(true);
            });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> updateElection(Election election) {
        Cache<UUID, Election> ec = cache.getElections();
        
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        ec.remove(election.getId());
        ec.add(election.getId(), election);
        electionsTable.removeWhere("id", election.getId())
            .thenRun(() -> {
                electionsTable.add(election);
                future.complete(true);
            });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteElection(Election election) {
        // remove the election from the cache
        cache.getElections().remove(election.getId());

        // remove all votes for this election from the cache
        List<Vote> votes = new ArrayList<Vote>();
        Cache<String, Vote> voteCache = cache.getVotes();
        for (Vote vote : voteCache.map().values()) {
            if (vote.getElectionId().equals(election.getId())) {
                votes.add(vote);
            }
        }
        for (Vote vote : votes) {
                voteCache.remove(vote.getCacheKey());
        }

        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        // remove the election from the database
        electionsTable
            .removeWhere("id", election.getId())
            .thenAccept((electionRemoveResult) -> {
                if (electionRemoveResult instanceof String) {
                    logger.severe(String.format("deleteElection[%s]: remove election: " + election.getName(), (String)electionRemoveResult));
                    future.complete(false);
                    return;
                }
    
                // remove all votes for this election from the database
                for (Vote vote : votes) {
                    this.deleteVote(vote)
                    .thenAccept((voteRemoveResult) -> {
                        if (voteRemoveResult instanceof String) {
                            logger.severe(String.format("deleteElection[%s]: remove vote: " + election.getName(), (String)voteRemoveResult));
                            future.complete(false);
                            return;
                        }
                        future.complete(true);
                    });
                }
        });

        return future;
    }

    @Override
    public CompletableFuture<?> deleteVote(Vote vote) {
        cache.getVotes().remove(vote.getCacheKey());

        UUID voteId = vote.getVoteId();
        return votesTable.removeWhere("id", voteId);
    }

    @Override
    public CompletableFuture<List<Vote>> getVotes() {
        CompletableFuture<List<Vote>> future = new CompletableFuture<>();

        this.getVotesTable().run(() -> {
                try {
                    PreparedStatement statement = votesTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", votesTable.getName()));
                    ResultSet set = statement.executeQuery();
                    List<Vote> votes = new ArrayList<>();
                    while (set.next()) {
                        UUID voteId = UUID.fromString(set.getString("id"));
                        UUID player = UUID.fromString(set.getString("player"));
                        String party = set.getString("party");
                        UUID electionId = UUID.fromString(set.getString("electionId"));

                        Vote vote = new Vote(voteId, player, party, electionId);
                        votes.add(vote);
                    }
                    future.complete(votes);
                } catch (SQLException e) {
                    e.printStackTrace();
                    future.complete(null);
                }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> vote(UUID player, Party party, Election election) {
        Vote vote = new Vote(UUID.randomUUID(), player, party.getName(), election.getId());
        return this.vote(vote);
    }

    @Override
    public CompletableFuture<Boolean> vote(Vote vote) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        getVotesTable().run(() -> {
                try {
                    Statement statement = connectionHandler.getConnection().createStatement();
                    String query = String.format("SELECT * FROM %s WHERE player = '%s' AND electionId = '%s';",
                        getVotesTable().getName(), vote.getPlayer().toString(), vote.getElectionId().toString());
                    ResultSet resultSet = statement.executeQuery(query);

                    if (resultSet.next()) {
                        future.complete(false);
                        return;
                    }

                    this.getVotesTable().add(vote);
                    cache.getVotes().add(vote.getCacheKey(), vote);
                    future.complete(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    future.complete(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    future.complete(false);
                }
            });

        return future;
    }

    @Override
    public CompletableFuture<List<ElectionBlock>> getElectionBlocks() {
        CompletableFuture<List<ElectionBlock>> future = new CompletableFuture<>();

        getBlocksTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                String query = String.format("SELECT * FROM %s;", getBlocksTable().getName());
                ResultSet resultSet = statement.executeQuery(query);

                List<ElectionBlock> electionBlocks = new ArrayList<>();
                Gson gson = new Gson();
                while (resultSet.next()) {
                    Type type = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> location = new HashMap<String, Object>(gson.fromJson(resultSet.getString("location"), type));
                    UUID world = UUID.fromString(resultSet.getString("world"));
                    String blockData = resultSet.getString("blockdata");

                    ElectionBlock electionBlock = new ElectionBlock(world, location, blockData);

                    electionBlocks.add(electionBlock);
                }

                future.complete(electionBlocks);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<ElectionBlock> createElectionBlock(UUID world, Map<String, Object> location, String blockData) {
        CompletableFuture<ElectionBlock> future = new CompletableFuture<>();

        blocksTable.find("location", location)
                .thenAccept((resultSet) -> {
                    try {
                        while (resultSet.next()) {
                            UUID w = UUID.fromString(resultSet.getString("world"));
                            if (w.equals(world)) {
                                future.complete(null);
                                return;
                            }
                        }

                        ElectionBlock electionBlock = new ElectionBlock(world, location, blockData);
                        blocksTable.add(electionBlock);
                        future.complete(electionBlock);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteElectionBlock(ElectionBlock electionBlock) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        // TODO(tadhunt): correctly handle different worlds
        blocksTable
            .removeWhere("location", electionBlock.getLocation())
            .thenAccept((o) -> {
                future.complete(true);
            });

        return future;
    }

    @Override
    public CompletableFuture<Map<String, DBHologram>> getHolograms() {
        CompletableFuture<Map<String, DBHologram>> future = new CompletableFuture<>();

        getHologramsTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                String query = String.format("SELECT * FROM %s;", getHologramsTable().getName());
                ResultSet resultSet = statement.executeQuery(query);

                HashMap<String, DBHologram> holograms = new HashMap<String, DBHologram>();

                while (resultSet.next()) {
                    String jsonString = resultSet.getString("json");

                    DBHologram hologram = new Gson().fromJson(jsonString, DBHologram.class);
                    holograms.put(hologram.getName(), hologram);
                }

                future.complete(holograms);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<DBHologram> createHologram(String name, String location, List<String> contents) {
        DBHologram hologram = new DBHologram(name, location, contents);
        CompletableFuture<DBHologram> future = new CompletableFuture<>();
        hologramsTable.find("name", name)
                .thenAccept((resultSet) -> {
                    try {
                        if (resultSet.next()) {
                                future.complete(null);
                                return;
                        }

                        hologramsTable.add(hologram);
                        future.complete(hologram);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteHologram(String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        hologramsTable
            .removeWhere("name", name)
            .thenAccept((o) -> {
                future.complete(true);
            });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> updateHologram(DBHologram hologram) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        hologramsTable
            .removeWhere("name", hologram.getName())
            .thenRun(() -> {
                hologramsTable.add(hologram);
                future.complete(true);
            });

        return future;
    }

    @Override
    public CompletableFuture<EClaim> createClaim(String name, Claim gclaim, UUID owner) {
        CompletableFuture<EClaim> future = new CompletableFuture<>();

        getClaimsTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                String query = String.format("SELECT * FROM %s WHERE name = '%s' OR id = '%s';", getClaimsTable().getName(), name, gclaim.getID().toString());
                ResultSet resultSet = statement.executeQuery(query);

                if (resultSet.next()) {
                        future.complete(null);
                        return;
                }

                EClaim eclaim = new EClaim(name, gclaim, owner);
                claimsTable.add(eclaim);
                future.complete(eclaim);
            } catch (SQLException e) {
                future.complete(null);
                e.printStackTrace();
                return;
            }

        });

        return future;
    }

    @Override
    public CompletableFuture<Map<String, EClaim>> getClaims() {
        CompletableFuture<Map<String, EClaim>> future = new CompletableFuture<>();

        getClaimsTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                String query = String.format("SELECT * FROM %s;", getClaimsTable().getName());
                ResultSet resultSet = statement.executeQuery(query);

                HashMap<String, EClaim> claims = new HashMap<String, EClaim>();

                while (resultSet.next()) {
                    EClaim claim = EClaim.fromResultSet(resultSet);
                    claims.put(claim.getName(), claim);
                }

                future.complete(claims);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<EClaim> getClaimById(Long id) {
        return getClaim("id", id.toString());
    }

    public CompletableFuture<EClaim> getClaimByName(String name) {
        return getClaim("name", name);
    }

    private CompletableFuture<EClaim> getClaim(String key, String value) {
        CompletableFuture<EClaim> future = new CompletableFuture<>();

        getClaimsTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                String query = String.format("SELECT * FROM %s WHERE %s = '%s';", getClaimsTable().getName(), key, value);
                ResultSet resultSet = statement.executeQuery(query);

                Integer nClaims = 0;
                EClaim claim = null;

                while (resultSet.next()) {
                    claim = EClaim.fromResultSet(resultSet);
                    nClaims++;
                }

                if (nClaims != 1) {
                    this.logger.severe(String.format("getClaim: got %d claims for key %s value %s (expected 0 or 1)", nClaims, key, value));
                    future.complete(null);
                }

                future.complete(claim);
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;

    }

    @Override
    public CompletableFuture<Boolean> deleteClaim(EClaim claim) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        claimsTable.removeWhere("id", claim.getId().toString())
            .thenAccept((result) -> {
                if (result instanceof Integer) {
                    future.complete(true);
                    return;
                }
                if (result instanceof String) {
                    this.logger.severe("deleteClaim: " + (String)result);
                } else {
                    this.logger.severe("deleteClaim: " + result.toString());
                }
                future.complete(false);
            });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> updateClaim(EClaim claim) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        claimsTable
            .removeWhere("id", claim.getId().toString())
            .thenRun(() -> {
                claimsTable.add(claim);
                future.complete(true);
            });

        return future;
    }
}