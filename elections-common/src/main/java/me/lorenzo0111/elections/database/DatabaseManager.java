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

package me.lorenzo0111.elections.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    public DatabaseManager(Logger logger, IAdvancedScheduler scheduler, CacheManager cache, ConfigurationNode config, IConnectionHandler handler) {
        this.logger = logger;
        this.connectionHandler = handler;

        this.tables(scheduler, cache, config);
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
        this.votesTable.setUnique("idx_vote_id", "id");

        // Parties
        List<Column> partiesColumns = new ArrayList<>();
        partiesColumns.add(new Column("id", "TEXT"));
        partiesColumns.add(new Column("name", "TEXT"));
        partiesColumns.add(new Column("owner", "TEXT"));
        partiesColumns.add(new Column("icon", "TEXT nullable"));
        partiesColumns.add(new Column("members", "TEXT"));
        this.partiesTable = new ETable(logger, scheduler, connectionHandler, "parties", partiesColumns);
        this.partiesTable.create();
        this.partiesTable.setUnique("idx_party_id", "id");

        // Elections
        List<Column> electionsColumns = new ArrayList<>();
        electionsColumns.add(new Column("id", "TEXT"));
        electionsColumns.add(new Column("name", "TEXT"));
        electionsColumns.add(new Column("parties", "TEXT"));
        electionsColumns.add(new Column("open", "INTEGER"));
        this.electionsTable = new ETable(logger, scheduler, connectionHandler, "elections", electionsColumns);
        this.electionsTable.create();
        this.electionsTable.setUnique("idx_election_id", "id");

        // Blocks
        List<Column> blocksColumns = new ArrayList<>();
        blocksColumns.add(new Column("id", "TEXT"));
        blocksColumns.add(new Column("world", "TEXT"));
        blocksColumns.add(new Column("location", "TEXT"));
        blocksColumns.add(new Column("blockdata", "TEXT"));
        this.blocksTable = new ETable(logger, scheduler, connectionHandler, "blocks", blocksColumns);
        this.blocksTable.create();
        this.blocksTable.setUnique("idx_block_id", "id");

        // Holograms
        List<Column> hologramsColumns = new ArrayList<>();
        hologramsColumns.add(new Column("id", "TEXT"));
        hologramsColumns.add(new Column("name", "TEXT"));
        hologramsColumns.add(new Column("location", "TEXT"));
        hologramsColumns.add(new Column("contents", "TEXT"));
        this.hologramsTable = new ETable(logger, scheduler, connectionHandler, "holograms", hologramsColumns);
        this.hologramsTable.create();
        this.hologramsTable.setUnique("idx_hologram_id", "id");

        // Claims
        List<Column> claimsColumns = new ArrayList<>();
        claimsColumns.add(new Column("id", "TEXT"));
        claimsColumns.add(new Column("name", "TEXT"));
        claimsColumns.add(new Column("gpid", "TEXT"));
        claimsColumns.add(new Column("owner", "TEXT"));
        this.claimsTable = new ETable(logger, scheduler, connectionHandler, "claims", claimsColumns);
        this.claimsTable.create();
        this.claimsTable.setUnique("idx_claim_id", "id");

        scheduler.async(new CacheTask(this.logger, this, cache));
    }

    @Override
    public void closeConnection() throws SQLException {
        connectionHandler.close();
    }

    @Override
    public CompletableFuture<List<Vote>> getVotes() {
        CompletableFuture<List<Vote>> future = new CompletableFuture<>();

        this.votesTable.run(() -> {
                try {
                    PreparedStatement statement = votesTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", votesTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    List<Vote> votes = new ArrayList<>();
                    while (resultSet.next()) {
                        Vote vote = Vote.fromResultSet(resultSet);
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
    public CompletableFuture<Boolean> updateVote(Vote vote) {
        return votesTable.addOrReplace(vote);
    }

    @Override
    public CompletableFuture<Boolean> deleteVote(Vote vote) {
        return votesTable.removeWhere("id", vote.getId());
    }

    @Override
    public CompletableFuture<Map<String, Party>> getParties() {
        CompletableFuture<Map<String, Party>> future = new CompletableFuture<>();

        this.partiesTable.run(() -> {
                try {
                    PreparedStatement statement = partiesTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", partiesTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    Map<String, Party> parties = new HashMap<>();
                    while (resultSet.next()) {
                        Party party = Party.fromResultSet(resultSet);
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

    @Override
    public CompletableFuture<Boolean> updateParty(Party party) {
        return partiesTable.addOrReplace(party);
    }

    @Override
    public CompletableFuture<Boolean> deleteParty(Party party) {
        return partiesTable.removeWhere("id", party.getId());
    }

    @Override
    public CompletableFuture<Map<String, Election>> getElections() {
        CompletableFuture<Map<String, Election>> future = new CompletableFuture<>();

        this.electionsTable.run(() -> {
                try {
                    PreparedStatement statement = electionsTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", electionsTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    Map<String, Election> elections = new HashMap<>();
                    while (resultSet.next()) {
                        Election election = Election.fromResultSet(resultSet);
                        elections.put(election.getName(), election);
                    }
                    future.complete(elections);
                } catch (SQLException e) {
                    e.printStackTrace();
                    future.complete(null);
                }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> updateElection(Election election) {
        return electionsTable.addOrReplace(election);
    }

    @Override
    public CompletableFuture<Boolean> deleteElection(Election election) {
        return electionsTable.removeWhere("id", election.getId());
    }

    @Override
    public CompletableFuture<List<ElectionBlock>> getBlocks() {
        CompletableFuture<List<ElectionBlock>> future = new CompletableFuture<>();

        this.blocksTable.run(() -> {
                try {
                    PreparedStatement statement = blocksTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", blocksTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    List<ElectionBlock> blocks = new ArrayList<>();
                    while (resultSet.next()) {
                        ElectionBlock block = ElectionBlock.fromResultSet(resultSet);
                        blocks.add(block);
                    }
                    future.complete(blocks);
                } catch (SQLException e) {
                    e.printStackTrace();
                    future.complete(null);
                }
        });

        return future;
    }
    
    @Override
    public CompletableFuture<Boolean> updateBlock(ElectionBlock block) {
        return blocksTable.addOrReplace(block);
    }

    @Override
    public CompletableFuture<Boolean> deleteBlock(ElectionBlock block) {
        return blocksTable.removeWhere("id", block.getId());
    }

    @Override
    public CompletableFuture<Map<String, DBHologram>> getHolograms() {
        CompletableFuture<Map<String, DBHologram>> future = new CompletableFuture<>();

        this.hologramsTable.run(() -> {
                try {
                    PreparedStatement statement = hologramsTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", hologramsTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    Map<String, DBHologram> holograms = new HashMap<>();
                    while (resultSet.next()) {
                        DBHologram dbholo = DBHologram.fromResultSet(resultSet);
                        holograms.put(dbholo.getName(), dbholo);
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
    public CompletableFuture<Boolean> updateHologram(DBHologram dbholo) {
        return hologramsTable.addOrReplace(dbholo);
    }

    @Override
    public CompletableFuture<Boolean> deleteHologram(DBHologram dbholo) {
        return hologramsTable.removeWhere("id", dbholo.getId());
    }

    @Override
    public CompletableFuture<Map<String, EClaim>> getClaims() {
        CompletableFuture<Map<String, EClaim>> future = new CompletableFuture<>();

        this.claimsTable.run(() -> {
                try {
                    PreparedStatement statement = claimsTable.getConnection().prepareStatement(String.format("SELECT * FROM %s;", claimsTable.getName()));
                    ResultSet resultSet = statement.executeQuery();
                    Map<String, EClaim> claims = new HashMap<>();
                    while (resultSet.next()) {
                        EClaim eclaim = EClaim.fromResultSet(resultSet);
                        claims.put(eclaim.getName(), eclaim);
                    }
                    future.complete(claims);
                } catch (SQLException e) {
                    e.printStackTrace();
                    future.complete(null);
                }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> updateClaim(EClaim eclaim) {
        return claimsTable.addOrReplace(eclaim);
    }

    @Override
    public CompletableFuture<Boolean> deleteClaim(EClaim eclaim) {
        return claimsTable.removeWhere("id", eclaim.getId());
    }
}