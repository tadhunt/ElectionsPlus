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

package me.lorenzo0111.elections;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.lorenzo0111.elections.api.IElectionsPlusAPI;
import me.lorenzo0111.elections.api.implementations.ElectionsPlusAPI;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.CacheEventHandler;
import me.lorenzo0111.elections.api.objects.DBHologram;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.api.objects.Vote;
import me.lorenzo0111.elections.cache.CacheManager;
import me.lorenzo0111.elections.commands.ElectionsCommand;
import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.DatabaseManager;
import me.lorenzo0111.elections.database.IDatabaseManager;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.listeners.JoinListener;
import me.lorenzo0111.elections.scheduler.BukkitScheduler;
import me.lorenzo0111.pluginslib.audience.BukkitAudienceManager;
import me.lorenzo0111.pluginslib.command.Customization;
import me.lorenzo0111.pluginslib.config.ConfigExtractor;
import me.lorenzo0111.pluginslib.database.connection.SQLiteConnection;
import me.lorenzo0111.pluginslib.updater.UpdateChecker;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.permission.Permission;

import org.bstats.bukkit.Metrics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ElectionsPlus extends JavaPlugin implements CacheEventHandler {
    private final CacheManager cache = new CacheManager(this);
    private boolean loaded;
    private IDatabaseManager manager;
    private static ElectionsPlus instance;
    private ElectionsPlusAPI api;

    private ConfigurationNode config;
    private ConfigurationNode messages;

    private Permission permissions;
    private HashMap<String, ElectionsHologram> holograms;
    private HolographicDisplaysAPI holoApi;
    private GriefPrevention gp;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        BukkitAudienceManager.init(this);
        new Metrics(this, 11735);

        this.load();
        
        Boolean checkForUpdates = config.node("update", "check").getBoolean(false);
        if (checkForUpdates) {
            this.getLogger().info("Update checks enabled.");
            Getters.updater(new UpdateChecker(new BukkitScheduler(this), this.getDescription().getVersion(), this.getName(), 93463, "https://www.spigotmc.org/resources/93463/", null, null));
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.getLogger().info("Placeholders enabled.");
            new ElectionsPlusPlaceholderExpansion(this).register();
        } else {
            this.getLogger().info("Placeholders disabled: add PlaceholderAPI plugin to enable.");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            this.holoApi = HolographicDisplaysAPI.get(this);
            this.getLogger().info("Holograms enabled");
        } else {
            this.holoApi = null;
            this.getLogger().info("Holograms disabled: add HolograpicDisplays plugin to enable.");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            this.gp = GriefPrevention.instance;
            this.getLogger().info("GriefPrevention integration enabled.");
            claimsInit();
        } else {
            this.gp = null;
            this.getLogger().info("GriefPrevention integration disabled: add GriefPrevention plugin to enable.");
        }
    }

    @Override
    public void onDisable() {
        GlobalMain.shutdown();

        if (!this.loaded) {
            this.getLogger().warning("Plugin is not initialized.");
            return;
        }

         try {
             this.getManager().closeConnection();
         } catch (SQLException e) {
             e.printStackTrace();
         }

         Bukkit.getScheduler().cancelTasks(this);

         Messages.close();
    }

    private void claimsInit() {
        IDatabaseManager manager = this.getManager();

        manager.getClaims()
            .thenAccept((eclaims) -> {
                for (EClaim eclaim : eclaims.values()) {
                    Claim gclaim = gp.dataStore.getClaim(eclaim.getId());

                    if (gclaim == null) {
                        manager.deleteClaim(eclaim)
                            .thenAccept((success) -> {
                                if (success) {
                                    this.getLogger().info(String.format("claimsInit: GP claim %d deleted, deleted claim %s", eclaim.getId(), eclaim.getName()));
                                } else {
                                    this.getLogger().info(String.format("claimsInit: GP claim %d deleted, failed to delete claim %s", eclaim.getId(), eclaim.getName()));
                                }
                            });
                        eclaim.delete();
                        continue;
                    }

                    claimTransfer(gclaim, eclaim, gclaim.getOwnerID());
                    claimPartyUpdate(eclaim);
                }
            });
    }
        
    public void claimTransfer(Claim gclaim, EClaim eclaim, UUID newOwner) {
        Long id = gclaim.getID();

        if (eclaim == null) {
            this.getLogger().info(String.format("claimTransfer[claim %d]: no corresponding elections claim", id));
            return;
        }

        UUID oldOwner = eclaim.getOwner();
        String ename = eclaim.getName();

        if (oldOwner == null && newOwner == null) {
            this.getLogger().info(String.format("claimTransfer[claim %d/%s]: owner (admin) unchanged", id, ename));
            return;
        } else if (oldOwner == null) {
            this.getLogger().info(String.format("claimTransfer[claim %d/%s]: owner changed admin -> %s/%s", id, ename, newOwner.toString(), Bukkit.getOfflinePlayer(newOwner).getName()));
        } else if (newOwner == null) {
            this.getLogger().info(String.format("claimTransfer[claim %d/%s]: owner changed %s/%s -> admin", id, ename, oldOwner.toString(), Bukkit.getOfflinePlayer(oldOwner).getName()));
        } else if (oldOwner.equals(newOwner)) {
            this.getLogger().info(String.format("claimTransfer[claim %d/%s]: owner (%s/%s) unchanged", id, ename, newOwner.toString(), Bukkit.getOfflinePlayer(newOwner).getName()));
            return;
        }

        eclaim.setOwner(newOwner);
    }

    private void claimPartyUpdate(EClaim eclaim) {
        String name = eclaim.getName();
        UUID owner = eclaim.getOwner();

        if (owner == null) {
            this.getManager().deleteParty(name);
            return;
        }

        Map<String, Party> parties = this.getCache().getParties().map();
        Party party = parties.get(name);

        if (party == null) {
            this.getManager().createParty(name, new UUID(0, 0))
                .thenAccept((newParty) -> {
                    if (newParty == null) {
                        this.getLogger().warning("party " + name + ": creation failed.");
                        return;
                    }
                    addPartyToOpenElections(newParty);
                });
            return;
        }

        addPartyToOpenElections(party);
    }

    private void addPartyToOpenElections(Party party) {
        Cache<UUID, Election> elections = this.getCache().getElections();
        for (Election election : elections.map().values()) {
            if (!election.isOpen()) {
                this.getLogger().info(String.format("election %s: skip (closed)", election.getName()));
                continue;
            }

            if (election.getParty(party.getName()) != null) {
                this.getLogger().info(String.format("election %s: party %s already added", election.getName(), party.getName()));
                continue;

            }

            election.addParty(party);
        }
    }

    // called by CacheTask every time the cache is reloaded
    public void onCacheReloaded() {
        if (this.holoApi != null) {
            holoReset();
        }
    }

    private void holoReset() {
        if (this.holograms == null) {
            // first time: create holograms

            this.holograms = new HashMap<String, ElectionsHologram>();

            this.getManager().getHolograms().thenAccept((dbholos) -> {
                this.getLogger().info(String.format("Initializing %d holograms", dbholos.size()));
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        for (DBHologram dbholo : dbholos.values()) {
                            ElectionsHologram hologram = new ElectionsHologram(this, this.holoApi, dbholo);
                            holograms.put(hologram.getName(), hologram);
                        }
                    } catch (Exception e) {
                        this.getLogger().severe("holoReset: " + e.toString());
                    }
                });
            });

            return;
        }

        // subsequent updates: refresh

        this.holoRefresh();
    }

    public void start() throws ConfigurateException {
        this.loaded = true;

        this.reload();

        this.getLogger().info("Loading scheduler...");
        GlobalMain.init(getDataFolder().toPath());

        if (config.node("rank", "enabled").getBoolean()) {
            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null)
                permissions = rsp.getProvider();
        }

        this.api = new ElectionsPlusAPI(this);
        Bukkit.getServicesManager().register(IElectionsPlusAPI.class, api, this, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        switch (getConfig().getString("database.type", "NULL").toUpperCase()) {
            case "SQLITE":
                try {
                    this.manager = new DatabaseManager(this.getLogger(), new BukkitScheduler(this), cache, config(), new SQLiteConnection(getDataFolder().toPath()));
                    Getters.database(manager);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
                break;
            case "MYSQL":
                try {
                    this.manager = new DatabaseManager(this.getLogger(), config, cache, getDataFolder().toPath(), new BukkitScheduler(this));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            default:
                this.getLogger().severe("Invalid database type");
                break;
        }

        Customization customization = new Customization(null,
                                            Messages.componentString(true, "errors", "command-not-found"),
                                            Messages.componentString(true, "errors", "help")
                                        );


        new ElectionsCommand(this, "elections", customization);
    }

    private void load() {
        try {
            this.start();
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
/*
        try {
            this.getLogger().info("Loading libraries..");
            this.getLogger().info("Note: This might take a few minutes on first run.");

            DependencyManager manager = new DependencyManager(getName(), getDataFolder().toPath());
            long time = manager.build();
            this.getLogger().info("Loaded all libraries in " + time + "ms");
            this.start();
        } catch (ReflectiveOperationException | URISyntaxException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
*/
    }

    public void reload() throws ConfigurateException {
        ConfigExtractor messagesExtractor = new ConfigExtractor(this.getClass(), this.getDataFolder(), "messages.yml");
        messagesExtractor.extract();
        this.messages = messagesExtractor.toConfigurate();

        ConfigExtractor configExtractor = new ConfigExtractor(this.getClass(), this.getDataFolder(), "config.yml");
        configExtractor.extract();
        this.config = configExtractor.toConfigurate();
        Messages.init(messages, config("prefix"), this);
    }

    public void win(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();

        if (!this.config("rank", "command").equalsIgnoreCase("none") && name != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.config("rank", "command").replace("%player%", name));
        }

        if (permissions == null)
            return;

        permissions.playerAddGroup(Bukkit.getWorlds().get(0).getName(), Bukkit.getOfflinePlayer(uuid), config.node("rank", "name").getString());
    }

    public ConfigurationNode config() {
        return this.config;
    }

    public String config(Object... path) {
        return this.config().node(path).getString("");
    }

    public IDatabaseManager getManager() {
        return manager;
    }

    public static ElectionsPlus getInstance() {
        return instance;
    }

    public ElectionsPlusAPI getApi() {
        return api;
    }

    public UpdateChecker getUpdater() {
        return Getters.updater();
    }

    public CacheManager getCache() {
        return cache;
    }

    public String array2string(String[] args, int start) {
        String result = "";
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                result = result + " ";
            }
            result = result + args[i];
        }

        return result;
    }

    public ArrayList<String> unquote(String[] args, int start) {
        String s = array2string(args, start);
        ArrayList<String> results = new ArrayList<String>();
        Boolean inQuote = false;

        String element = "";
        Character quote = Character.valueOf('"');
        Character space = Character.valueOf(' ');
        Character last = Character.valueOf('x');
        
        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);

            if (inQuote) {
                if (c.equals(quote)) {
                    inQuote = false;
                    results.add(element);
                    element = "";
                } else {
                    element += c;
                }
            } else {
                if (c.equals(quote)) {
                    inQuote = true;
                } else if (c.equals(space)) {
                    if(last.equals(quote)) {
                        // strip it
                    } else {
                        results.add(element);
                        element = "";
                    }
                } else {
                    element += c;
                }
            }
            last = c;
        }

        if (element.length() > 0) {
            results.add(element);
        }

        return results;
    }

    public ElectionsHologram holoCreate(String name, Location location, List<String> contents) {
        if (this.holoApi == null) {
            this.getLogger().severe("HolograpicDisplays plugin not enabled");
            return null;
        }

        if (holograms.get(name) != null) {
            this.getLogger().severe("holoCreate: duplicate name: " + name);
            return null;
        }

        ElectionsHologram hologram = new ElectionsHologram(this, this.holoApi, name, location, contents, true);

        holograms.put(hologram.getName(), hologram);

        return hologram;
    }

    public ElectionsHologram holoGet(String name) {
        return holograms.get(name);
    }

    public Boolean holoDelete(String name) {
        ElectionsHologram holo = holograms.remove(name);
        if (holo == null) {
            return false;
        }

        holo.delete();

        return true;
    }

    public Collection<ElectionsHologram> holoList() {
        return holograms.values();
    }

    public void holoRefresh() {
        try {
            for (ElectionsHologram holo : holograms.values()) {
                holo.refresh();
            }
        } catch(Exception e) {
            this.getLogger().severe("holoRefresh: EXCEPTION: " + e.toString());
        }
    }

    public Map<UUID, ElectionStatus> getElectionStatuses() {
        HashMap<UUID, ElectionStatus> statuses = new HashMap<UUID, ElectionStatus>();

        Cache<UUID, Election> elections = this.getCache().getElections();
        if (elections == null) {
            this.getLogger().severe("getElectionStatuses: null elections");
            return statuses;
        }

        for (Election election : elections.map().values()) {
            statuses.put(election.getId(), new ElectionStatus(election));
        }

        Cache<String, Vote> votes = this.getCache().getVotes();
        if (votes == null) {
            this.getLogger().severe("getElectionStatuses: null votes");
            return statuses;
        }

        for (Vote vote : votes.map().values()) {
            UUID electionId = vote.getElectionId();
            String partyName = vote.getParty();

            ElectionStatus status = statuses.get(electionId);
            if (status == null) {
                this.getLogger().warning(String.format("vote for election %s party %s: election not found", electionId.toString(), partyName));
                continue;
            }

            status.addVote(partyName);
        }

        return statuses;
    }

    public ElectionStatus getElectionStatus(String electionName) {
        Map<UUID, ElectionStatus> statuses = this.getElectionStatuses();

        for (ElectionStatus status : statuses.values()) {
            if (status.getElection().getName().equals(electionName)) {
                return status;
            }
        }

        return null;
    }

    public GriefPrevention getGriefPrevention() {
        return gp;
    }
}