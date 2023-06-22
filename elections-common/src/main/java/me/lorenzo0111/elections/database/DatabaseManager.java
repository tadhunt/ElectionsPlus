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
        votesColumns.add(new Column("voteId", "TEXT"));
        votesColumns.add(new Column("player", "TEXT"));
        votesColumns.add(new Column("party", "TEXT"));
        votesColumns.add(new Column("election", "TEXT"));
        this.votesTable = new ETable(scheduler, connectionHandler, "votes", votesColumns);
        this.votesTable.create();

        // Parties
        List<Column> partiesColumns = new ArrayList<>();
        partiesColumns.add(new Column("owner", "TEXT"));
        partiesColumns.add(new Column("name", "TEXT"));
        partiesColumns.add(new Column("members", "TEXT"));
        partiesColumns.add(new Column("icon", "TEXT nullable"));
        this.partiesTable = new ETable(scheduler, connectionHandler, "parties", partiesColumns);
        this.partiesTable.create();

        // Elections
        List<Column> electionsColumns = new ArrayList<>();
        electionsColumns.add(new Column("name", "TEXT"));
        electionsColumns.add(new Column("parties", "TEXT"));
        electionsColumns.add(new Column("open", "INTEGER"));
        this.electionsTable = new ETable(scheduler, connectionHandler, "elections", electionsColumns);
        this.electionsTable.create();

        // Blocks
        List<Column> blocksColumns = new ArrayList<>();
        blocksColumns.add(new Column("world", "TEXT"));
        blocksColumns.add(new Column("location", "TEXT"));
        blocksColumns.add(new Column("blockdata", "TEXT"));
        this.blocksTable = new ETable(scheduler, connectionHandler, "blocks", blocksColumns);
        this.blocksTable.create();

        // Holograms
        List<Column> hologramsColumns = new ArrayList<>();
        hologramsColumns.add(new Column("name", "TEXT"));
        hologramsColumns.add(new Column("json", "TEXT"));
        this.hologramsTable = new ETable(scheduler, connectionHandler, "holograms", hologramsColumns);
        this.hologramsTable.create();

        scheduler.repeating(new CacheTask(this, cache), 60 * 20L, config.node("cache-duration").getInt(5), TimeUnit.MINUTES);
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

    @Override
    public CompletableFuture<Election> createElection(String name, List<Party> parties) {
        CompletableFuture<Election> future = new CompletableFuture<>();

        Election election = new Election(name, parties, true);

        this.getElectionsTable()
                .find("name", name)
                .thenAccept((result) -> {
                    try {
                        if (result.next()) {
                            future.complete(null);
                            return;
                        }

                        this.getElectionsTable().add(election);
                        cache.getElections().add(election.getName(), election);
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

        getParties()
                .thenAccept((allParties) -> getElectionsTable().run(() -> {
                    logger.severe(String.format("getElections: got %d parties", allParties.size()));
                    try {
                        Statement statement = connectionHandler.getConnection().createStatement();
                        ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s;", getElectionsTable().getName()));

                        List<Election> elections = new ArrayList<>();
                        Gson gson = new Gson();
                        while (resultSet.next()) {
                            String name = resultSet.getString("name");
                            Boolean open = resultSet.getInt("open") != 0;

                            List<Party> parties = new ArrayList<>();
                            Type type = new TypeToken<ArrayList<String>>() {}.getType();
                            List<String> partyNames = new ArrayList<>(gson.fromJson(resultSet.getString("parties"), type));
                            partyNames.forEach((n) -> allParties.stream()
                                    .filter((p) -> p.getName().equals(n))
                                    .findFirst()
                                    .ifPresent(parties::add));

                            Election election = new Election(name, parties, open);

                            elections.add(election);
                        }
                        logger.severe(String.format("getElections: got %d elections", elections.size()));

                        future.complete(elections);
                    } catch (SQLException e) {
                        logger.severe("SQL EXCEPTION: " + e.toString());
                        e.printStackTrace();
                        future.complete(null);
                    } catch (Exception e) {
                        logger.severe("EXCEPTION: " + e.toString());
                        future.complete(null);
                    }
                }));

        return future;
    }

    @Override
    public CompletableFuture<List<Party>> getParties() {
        CompletableFuture<List<Party>> future = new CompletableFuture<>();

        getPartiesTable().run(() -> {
            try {
                Statement statement = connectionHandler.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s;", getPartiesTable().getName()));

                List<Party> parties = new ArrayList<>();
                Gson gson = new Gson();
                while (resultSet.next()) {
                    Type type = new TypeToken<ArrayList<UUID>>() {}.getType();
                    List<UUID> members = new ArrayList<>(gson.fromJson(resultSet.getString("members"), type));
                    Party party = new Party(resultSet.getString("name"), UUID.fromString(resultSet.getString("owner")), members);

                    if (resultSet.getString("icon") != null)
                        party.setIcon(resultSet.getString("icon"));

                    parties.add(party);
                }

                future.complete(parties);
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
    public void deleteParty(String name) {
        cache.getParties().remove(name);
        partiesTable.removeWhere("name", name);
    }

    @Override
    public void deleteParty(Party party) {
        cache.getParties().remove(party.getName());
        partiesTable.removeWhere("name", party);
    }

    @Override
    public void updateParty(Party party) {
        cache.getParties().remove(party.getName());
        cache.getParties().add(party.getName(), party);
        partiesTable.removeWhere("name", party)
                .thenRun(() -> partiesTable.add(party));
    }

    @Override
    public void updateElection(Election election) {
        cache.getElections().remove(election.getName());
        cache.getElections().add(election.getName(), election);
        electionsTable.removeWhere("name", election)
                .thenRun(() -> electionsTable.add(election));
    }

    @Override
    public void deleteElection(Election election) {
        String name = election.getName();

        // remove the election from the cache
        cache.getElections().remove(name);

        // remove all votes for this election from the cache
        List<Vote> votes = new ArrayList<Vote>();
        Cache<String, Vote> voteCache = cache.getVotes();
        for (Vote vote : voteCache.map().values()) {
            if (vote.getElection().equals(name)) {
                votes.add(vote);
            }
        }
        for (Vote vote : votes) {
                voteCache.remove(vote.getCacheKey());
        }

        // remove the election from the database
        electionsTable.removeWhere("name", election).thenAccept((electionRemoveResult) -> {
            if (electionRemoveResult instanceof String) {
                logger.severe(String.format("deleteElection[%s]: remove election: " + name, (String)electionRemoveResult));
            }
        });

        // remove all votes for this election from the database
        for (Vote vote : votes) {
            this.deleteVote(vote).thenAccept((voteRemoveResult) -> {
                if (voteRemoveResult instanceof String) {
                    logger.severe(String.format("deleteElection[%s]: remove vote: " + name, (String)voteRemoveResult));
                }
            });
        }
    }

    @Override
    public CompletableFuture<?> deleteVote(Vote vote) {
        cache.getVotes().remove(vote.getCacheKey());

        UUID voteId = vote.getVoteId();
        return votesTable.removeWhere("voteId", voteId);
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
                        UUID voteId = UUID.fromString(set.getString("voteId"));
                        UUID player = UUID.fromString(set.getString("player"));
                        String party = set.getString("party");
                        String election = set.getString("election");

                        Vote vote = new Vote(voteId, player, party, election);
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
        Vote vote = new Vote(UUID.randomUUID(), player, party.getName(), election.getName());
        return this.vote(vote);
    }

    @Override
    public CompletableFuture<Boolean> vote(Vote vote) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        this.getVotesTable()
                .find("player", vote.getPlayer())
                .thenAccept((set) -> {
                    try {
                        while (set.next()) {
                            if (set.getString("election").equals(vote.getElection())) {
                                future.complete(false);
                                return;
                            }
                        }

                        this.getVotesTable().add(vote);
                        cache.getVotes().add(vote.getCacheKey(), vote);
                        future.complete(true);
                    } catch (SQLException e) {
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
        CompletableFuture<ElectionBlock> electionBlockFuture = new CompletableFuture<>();

        blocksTable.find("location", location)
                .thenAccept((resultSet) -> {
                    try {
                        while (resultSet.next()) {
                            UUID w = UUID.fromString(resultSet.getString("world"));
                            if (w.equals(world)) {
                                electionBlockFuture.complete(null);
                                return;
                            }
                        }

                        ElectionBlock electionBlock = new ElectionBlock(world, location, blockData);
                        blocksTable.add(electionBlock);
                        electionBlockFuture.complete(electionBlock);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        return electionBlockFuture;
    }

    @Override
    public void deleteElectionBlock(ElectionBlock electionBlock) {
        // TODO(tadhunt): correctly handle different worlds
        blocksTable.removeWhere("location", electionBlock.getLocation());
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
    public void deleteHologram(String name) {
        hologramsTable.removeWhere("name", name);
    }

    @Override
    public void updateHologram(DBHologram hologram) {
        hologramsTable
            .removeWhere("name", hologram.getName())
            .thenRun(() -> hologramsTable.add(hologram));
    }
}