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

import me.lorenzo0111.elections.api.IElectionsPlusAPI;
import me.lorenzo0111.elections.api.implementations.ElectionsPlusAPI;
import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.CacheEventHandler;
import me.lorenzo0111.elections.api.objects.DBHologram;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.ElectionBlock;
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.api.objects.Vote;
import me.lorenzo0111.elections.cache.CacheManager;
import me.lorenzo0111.elections.commands.ElectionsCommand;
import me.lorenzo0111.elections.constants.Getters;
import me.lorenzo0111.elections.database.DatabaseManager;
import me.lorenzo0111.elections.database.IDatabaseManager;
import me.lorenzo0111.elections.handlers.Messages;
import me.lorenzo0111.elections.listeners.JoinListener;
import me.lorenzo0111.elections.listeners.VoteBlockListener;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private final CacheManager cache = new CacheManager(this.getLogger(), this);
    private boolean loaded;
    private IDatabaseManager manager;
    private static ElectionsPlus instance;
    private ElectionsPlusAPI api;

    private ConfigurationNode config;
    private ConfigurationNode messages;

    private Permission permissions;
    private HashMap<String, IElectionsHologram> holograms;
    private ElectionsHologramAPI holoApi;
    private GriefPrevention gp;
    private VoteBlockListener voteBlockListener;

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

        this.holoApi = new ElectionsHologramAPI(this);

        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            this.gp = GriefPrevention.instance;
            this.getLogger().info("GriefPrevention integration enabled.");
        } else {
            this.gp = null;
            this.getLogger().info("GriefPrevention integration disabled: add GriefPrevention plugin to enable.");
        }

        Getters.cache(this.cache);
        Getters.database(this.manager);
    }

    @Override
    public void onDisable() {
        GlobalMain.shutdown();

        if (!this.loaded) {
            this.getLogger().warning("Plugin is not initialized.");
            return;
        }

         try {
             this.manager.closeConnection();
         } catch (SQLException e) {
             e.printStackTrace();
         }

         Bukkit.getScheduler().cancelTasks(this);   // this could cause some queued up database updates to get lost...

         Messages.close();
    }

    private void claimsInit() {
        claimOwnersInit();
        claimPartiesRefresh();
    }
        
    private void claimOwnersInit() {
        Cache<UUID, EClaim> eclaims = getCache().getClaims();
        Cache<UUID, Party> parties = getCache().getParties();

        for (EClaim eclaim : Map.copyOf(eclaims.map()).values()) {
            Claim gclaim = gp.dataStore.getClaim(eclaim.getGpId());

            if (gclaim == null) {
                eclaims.remove(eclaim.getId());
                Party party = parties.findByName(eclaim.getName());
                if (party != null) {
                    parties.remove(party.getId());
                }
                continue;
            }

            claimTransfer(gclaim, eclaim, gclaim.getOwnerID());
        }

        eclaims.persist();
        parties.persist();
    }

    public void claimTransfer(Claim gclaim, EClaim eclaim, UUID newOwner) {
        Long id = gclaim.getID();

        if (eclaim == null) {
            this.getLogger().info(String.format("claimTransfer[claim %d]: no corresponding elections claim", id));
            return ;
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

        cache.getClaims().persist();

        claimPartiesUpdate(eclaim);
    }

    public void claimPartiesRefresh() {
        Cache<UUID, EClaim> eclaims = getCache().getClaims();

        for (EClaim eclaim : eclaims.map().values()) {
            claimPartiesUpdate(eclaim);
        }
    }
            
    public void claimPartiesUpdate(EClaim eclaim) {
        Cache<UUID, Election> elections = getCache().getElections();
        Cache<UUID, Party> parties = getCache().getParties();

        if (eclaim.getOwner() == null) {
            Party party = parties.findByName(eclaim.getName());
            if (party != null) {
                deleteParty(party);     // mutates and persists changes to elections and parties
                getLogger().info(String.format("claimPartyUpdate: claim %s: owned by admin, deleted party %s.", eclaim.getName(), party.getName()));
            }
            return;
        }

        Party party = parties.findByName(eclaim.getName());
        if (party == null) {
            party = new Party(UUID.randomUUID(), eclaim.getName(), new UUID(0,0), true);
            parties.add(party.getId(), party);
            parties.persist();

            getLogger().info(String.format("claimPartyUpdate: claim %s: created new party.", eclaim.getName()));
        }

        boolean dirty = false;
        for (Election election : elections.map().values()) {
            if (!election.isOpen()) {
                this.getLogger().info(String.format("claimPartyUpdate: claim %s party %s election %s: skip (election closed).", eclaim.getName(), party.getName(), election.getName()));
                continue;
            }

            if (election.partyExists(party.getId())) {
                this.getLogger().info(String.format("claimPartyUpdate: claim %s party %s election %s: skip (party already present).", eclaim.getName(), party.getName(), election.getName()));
                continue;
            }

            election.addParty(party.getId());
            dirty = true;
            this.getLogger().info(String.format("claimPartyUpdate: claim %s party %s election %s: added party.", eclaim.getName(), party.getName(), election.getName()));
        }

        if (dirty) {
            elections.persist();
        }
    }

    // called by CacheTask when the cache is ready
    public void onCacheReloaded() {
        try {
            cleanCache();

            if(this.gp != null) {
                claimsInit();
            }

            this.voteBlockListener = new VoteBlockListener(this);

            if (this.holoApi.enabled()) {
                holoReset();
            }

            Customization customization = new Customization(null,
                                                Messages.componentString(true, "errors", "command-not-found"),
                                                Messages.componentString(true, "errors", "help")
                                            );

            new ElectionsCommand(this, "elections", customization);
        } catch (Exception e) {
            getLogger().severe(e.toString());
            e.printStackTrace();
        }
    }

    private void cleanCache() {
        try {
            cleanParties();
            cleanElections();
            cleanVotes();
            // claims handled by claimsInit()
            // holograms handled by holoReset()
            cleanBlocks();
        } catch (Exception e) {
            getLogger().severe("cleanCache: " + e.toString());
        }
    }

    private void cleanParties() {
        // currently a noop - nothing can go bad because it doesn't link to any other entities
    }

    private void cleanBlocks() {
        Cache<UUID, ElectionBlock> blocks = cache.getBlocks();

        boolean dirty = false;
        for (ElectionBlock block : Map.copyOf(blocks.map()).values()) {
            if (!blockExists(block)) {
                blocks.remove(block.getId());
                dirty = true;
                getLogger().info(String.format("Removed electionblock at location %s (blockdata changed)", block.getLocation().toString()));
            }
        }

        if (dirty) {
            blocks.persist();
        }
    }

    private boolean blockExists(ElectionBlock eblock) {
        World world = Bukkit.getWorld(eblock.getWorld());
        if (world == null) {
            return false;
        }

        Location location = Location.deserialize(eblock.getLocation());
        Block block = world.getBlockAt(location);
        String blockData = block.getBlockData().getAsString();

        if (eblock.getBlockData().equals(blockData)) {
            return true;
        }

        return false;
    }

    private void cleanElections() {
        Cache<UUID, Election> elections = cache.getElections();
        Cache<UUID, Party> parties = cache.getParties();

        boolean dirty = false;

        for(Election election : elections.map().values()) {
            for (UUID partyId : Map.copyOf(election.getParties()).keySet()) {
                Party party = parties.get(partyId);
                if (party == null) {
                    election.deleteParty(partyId);
                    dirty = true;
                    getLogger().info(String.format("Removed party %s from election %s (party does not exist)", partyId.toString(), election.getName()));
                }
            }
        }

        if (dirty) {
            elections.persist();
        }
    }

    private void cleanVotes() {
        Cache<UUID, Election> elections = cache.getElections();
        Cache<UUID, Party> parties = cache.getParties();
        Cache<UUID, Vote> votes = cache.getVotes();

        boolean dirty = false;
        for (Vote vote : Map.copyOf(votes.map()).values()) {
            Election election = elections.get(vote.getElectionId());
            if (election == null) {
                votes.remove(vote.getId());
                dirty = true;
                getLogger().info(String.format("Removed vote %s in election %s (election does not exist)", vote.getId().toString(), vote.getElectionId()));
                continue;
            }

            Party party = parties.get(vote.getParty());
            if (party == null) {
                votes.remove(vote.getId());
                dirty = true;
                getLogger().info(String.format("Removed vote %s for party %s in election %s (party does not exist)", vote.getId().toString(), vote.getParty(), election.getName()));
                continue;
            }
        }

        if(dirty) {
            votes.persist();
        }
    }

    private void holoReset() {
        if (this.holograms == null) {
            // first time: create holograms

            this.holograms = new HashMap<String, IElectionsHologram>();

            Cache<UUID, DBHologram> dbholograms = getCache().getHolograms();

            Bukkit.getScheduler().runTask(this, () -> {
                this.getLogger().info(String.format("Initializing %d holograms", dbholograms.size()));
                for(DBHologram dbholo : dbholograms.map().values()) {
                    try {
                        IElectionsHologram hologram = this.holoApi.create(dbholo);
                        holograms.put(hologram.getName(), hologram);
                    } catch (Exception e) {
                        this.getLogger().severe("holoReset: " + e.toString());
                    }
                }
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

    public IDatabaseManager getDatabaseManager() {
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

    public IElectionsHologram holoCreate(String name, Location location, List<String> contents) {
        if (!this.holoApi.enabled()) {
            this.getLogger().severe("holograms plugin not enabled");
            return null;
        }

        if (holograms.get(name) != null) {
            this.getLogger().severe("holoCreate: duplicate name: " + name);
            return null;
        }

        IElectionsHologram hologram = this.holoApi.create(name, location, contents, true);

        holograms.put(hologram.getName(), hologram);

        return hologram;
    }

    public IElectionsHologram holoGet(String name) {
        return holograms.get(name);
    }

    public Boolean holoDelete(String name) {
        IElectionsHologram holo = holograms.remove(name);
        if (holo == null) {
            return false;
        }

        holo.delete();

        return true;
    }

    public Collection<IElectionsHologram> holoList() {
        return holograms.values();
    }

    public void holoRefresh() {
        try {
            for (IElectionsHologram holo : holograms.values()) {
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
            this.getLogger().info("getElectionStatuses: null elections");
            return statuses;
        }

        for (Election election : elections.map().values()) {
            statuses.put(election.getId(), new ElectionStatus(election));
        }

        Cache<UUID, Vote> votes = this.getCache().getVotes();
        Cache<UUID, Party> parties = this.getCache().getParties();

        if (votes == null) {
            this.getLogger().info("getElectionStatuses: null votes");
            return statuses;
        }

        for (Vote vote : votes.map().values()) {
            UUID electionId = vote.getElectionId();
            UUID partyId = vote.getParty();

            ElectionStatus status = statuses.get(electionId);
            if (status == null) {
                String partyName = partyId.toString();
                Party party = parties.get(partyId);
                if (party != null) {
                    partyName = party.getName();
                }
                this.getLogger().warning(String.format("vote for election %s party %s: election not found", electionId.toString(), partyName));
                continue;
            }

            status.addVote(partyId);
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

    public void deleteParty(Party party) {
        Cache<UUID, Election> elections = getCache().getElections();
        Cache<UUID, Party> parties = getCache().getParties();

        parties.remove(party.getId());
        parties.persist();

        boolean edirty = false;
        for(Election election : elections.map().values()) {
            if (election.getParties().containsKey(party.getId())) {
                election.deleteParty(party.getId());
                edirty = true;
            }
        }

        if (edirty) {
            elections.persist();
        }
    }

    public Vote findVote(Cache<UUID, Vote> votes, Election election, UUID playerId) {
        for (Vote vote : votes.map().values()) {
            if (vote.getElectionId().equals(election.getId()) && vote.getPlayer().equals(playerId)) {
                return vote;
            }
        }

        return null;
    }

    public VoteBlockListener getVoteBlockListener() {
        return voteBlockListener;
    }

    public EClaim findClaimByGpId(Long gpid) {
        Cache<UUID, EClaim> eclaims = cache.getClaims();
        for (EClaim eclaim : eclaims.map().values()) {
            if (eclaim.getGpId().equals(gpid)) {
                return eclaim;
            }
        }

        return null;
    }

    /*
     * NOTE(tadhunt): This is a hack to make sure that we have a non-null OfflinePlayer to
     * use for the ItemBuilder API, otherwise we end up crashing with weird exceptions.
     */
    public OfflinePlayer getPartyOwner(Party party, OfflinePlayer defaultOwner) {
        UUID nilUuid = new UUID(0, 0);
        UUID partyOwnerUuid = party.getOwner();

        if (partyOwnerUuid.equals(nilUuid)) {
            return defaultOwner;
        }

        try {
            return Bukkit.getOfflinePlayer(party.getOwner());
        } catch(Exception e) {
            return defaultOwner;
        }
    }

}