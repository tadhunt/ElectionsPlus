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
import me.lorenzo0111.elections.api.objects.Election;
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
import me.lorenzo0111.pluginslib.dependency.DependencyManager;
import me.lorenzo0111.pluginslib.updater.UpdateChecker;

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
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
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
    HolographicDisplaysAPI holoApi;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        BukkitAudienceManager.init(this);
        new Metrics(this, 11735);

        this.load();
        
        Boolean checkForUpdates = config.node("update", "check").getBoolean(false);
        if (checkForUpdates) {
            this.getLogger().info("Enabling update checks...");
            Getters.updater(new UpdateChecker(new BukkitScheduler(this), this.getDescription().getVersion(), this.getName(), 93463, "https://www.spigotmc.org/resources/93463/", null, null));
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.getLogger().info("Enabling PlaceholderAPI...");
            new ElectionsPlusPlaceholderExpansion(this).register();
        }

        this.holoInit();
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

    // called by CacheTask every time the cache is reloaded
    public void onCacheInitialized() {
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
            this.getLogger().info("Loading libraries..");
            this.getLogger().info("Note: This might take a few minutes on first run.");

            DependencyManager manager = new DependencyManager(getName(), getDataFolder().toPath());
            long time = manager.build();
            this.getLogger().info("Loaded all libraries in " + time + "ms");
            this.start();
        } catch (ReflectiveOperationException | URISyntaxException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
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

    private void holoInit() {
        this.holograms = new HashMap<String, ElectionsHologram>();

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            this.getLogger().info("holograms disabled: HolograpicDisplays plugin not enabled");
            this.holoApi = null;
            return;
        }

        this.getLogger().info("Holograms enabled");

        this.holoApi = HolographicDisplaysAPI.get(this);

        this.getManager().getHolograms().thenAccept((dbholos) -> {
            this.getLogger().info(String.format("Initializing %d holograms", dbholos.size()));
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    for (DBHologram dbholo : dbholos.values()) {
                        ElectionsHologram hologram = new ElectionsHologram(this, this.holoApi, dbholo);
                        holograms.put(hologram.getName(), hologram);
                    }
                } catch (Exception e) {
                    this.getLogger().severe("holoInit: " + e.toString());
                }
            });
        });
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

    public Map<String, ElectionStatus> getElectionStatuses() {
        HashMap<String, ElectionStatus> statuses = new HashMap<String, ElectionStatus>();

        Cache<String, Election> elections = this.getCache().getElections();
        if (elections == null) {
            this.getLogger().severe("getElectionStatuses: null elections");
            return statuses;
        }

        for (Election election : elections.map().values()) {
            statuses.put(election.getName(), new ElectionStatus(election));
        }

        Cache<String, Vote> votes = this.getCache().getVotes();
        if (votes == null) {
            this.getLogger().severe("getElectionStatuses: null votes");
            return statuses;
        }

        for (Vote vote : votes.map().values()) {
            String electionName = vote.getElection();
            String partyName = vote.getParty();

            ElectionStatus status = statuses.get(electionName);
            if (status == null) {
                this.getLogger().warning(String.format("vote for election %s party %s: election not found", electionName, partyName));
                continue;
            }

            status.addVote(partyName);
        }

        return statuses;
    }

    public ElectionStatus getElectionStatus(String electionName) {
        Map<String, ElectionStatus> statuses = this.getElectionStatuses();

        return statuses.get(electionName);
    }
}
