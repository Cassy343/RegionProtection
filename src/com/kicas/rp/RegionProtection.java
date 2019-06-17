package com.kicas.rp;

import com.kicas.rp.command.CommandHandler;
import com.kicas.rp.data.DataManager;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.event.EntityEventHandler;
import com.kicas.rp.event.RegionToolHandler;
import com.kicas.rp.event.PlayerEventHandler;
import com.kicas.rp.event.WorldEventHandler;
import com.kicas.rp.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * This is the main plugin class for the Region Protection plugin. All non-utility features of the plugin can be
 * accessed through this class.
 */
public class RegionProtection extends JavaPlugin {
    private final CommandHandler commandHandler;
    private final DataManager dataManager;
    private Material claimCreationTool, claimViewer;
    private double claimBlocksGainedPerMinute;

    private static RegionProtection instance;

    public RegionProtection() {
        this.commandHandler = new CommandHandler();
        this.dataManager = new DataManager(getDataFolder());
        instance = this;
    }

    @Override
    public void onEnable() {
        initConfig();
        commandHandler.registerCommands();
        dataManager.load();

        Bukkit.getPluginManager().registerEvents(commandHandler, this);
        Bukkit.getPluginManager().registerEvents(dataManager, this);
        Bukkit.getPluginManager().registerEvents(new RegionToolHandler(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(), this);
        Bukkit.getPluginManager().registerEvents(new EntityEventHandler(), this);
        Bukkit.getPluginManager().registerEvents(new WorldEventHandler(), this);

        // Automatic claim block gaining
        if(claimBlocksGainedPerMinute > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                Bukkit.getOnlinePlayers().stream().map(dataManager::getPlayerSession)
                        .forEach(ps -> ps.addClaimBlocks(claimBlocksGainedPerMinute));
            }, 0L, 60L * 20L);
        }

        // Expire claims if they are older then the time given in the config (in days). The age of a claim is determined
        // by subtracting the last login time of the most recently active trustee with at least container trust from the
        // current time.
        final long claimExpirationTime = getConfig().getInt("general.claim-expiration-time") * 24L * 60L * 60L * 1000L;
        if(claimExpirationTime > 0 && !getConfig().getBoolean("general.enable-claim-stealing")) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getWorlds().forEach(world -> {
                    dataManager.getRegionsInWorld(world).stream().filter(region ->
                            region.hasExpired(claimExpirationTime)).forEach(region ->
                            dataManager.tryDeleteRegion(null, region, true));
                });
            }, 100L);
        }
    }

    @Override
    public void onDisable() {
        dataManager.save();
    }

    public static RegionProtection getInstance() {
        return instance;
    }

    public static DataManager getDataManager() {
        return instance.dataManager;
    }

    public static FileConfiguration getRPConfig() {
        return instance.getConfig();
    }

    public static Material getClaimCreationTool() {
        return instance.claimCreationTool;
    }

    public static Material getClaimViewerTool() {
        return instance.claimViewer;
    }

    public static void log(Object x) {
        Bukkit.getLogger().info("[RegionProtection] " + Objects.toString(x));
    }

    public static void error(Object x) {
        String msg = Objects.toString(x);
        Bukkit.getLogger().severe("[RegionProtection] " + msg);
    }

    private void initConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("general.claim-creation-item", Material.GOLDEN_SHOVEL.name());
        config.addDefault("general.claim-viewer", Material.STICK.name());
        config.addDefault("general.minimum-claim-size", 100);
        config.addDefault("general.claim-blocks-gained-per-hour", 512);
        config.addDefault("general.claim-expiration-time", 60);
        config.addDefault("general.enable-claim-stealing", false);

        config.addDefault("player.invincible", false);
        config.addDefault("player.hostile-damage", true);
        config.addDefault("player.animal-damage", false);
        config.addDefault("player.potion-splash", true);
        config.addDefault("player.chest-access", true);
        config.addDefault("player.pvp", false);
        config.addDefault("player.bed-enter", true);

        config.addDefault("entity.mob-grief", false);
        config.addDefault("entity.enderman-block-damage", false);

        config.addDefault("world.tnt-explosions", false);
        config.addDefault("world.overlap", false);
        config.addDefault("world.water-flow", true);
        config.addDefault("world.lava-flow", true);
        config.addDefault("world.snow-change", true);
        config.addDefault("world.ice-change", true);
        config.addDefault("world.coral-death", true);
        config.addDefault("world.leaf-decay", true);

        config.options().copyDefaults(true);
        saveConfig();

        // Convert some of the config values into usable types
        claimCreationTool = Utils.safeValueOf(Material::valueOf, config.getString("general.claim-creation-item"));
        if(claimCreationTool == null) {
            log("Invalid material found in config under general.claim-creation-item: " +
                    config.getString("general.claim-creation-item"));
            claimCreationTool = Material.GOLDEN_SHOVEL;
        }
        claimViewer = Utils.safeValueOf(Material::valueOf, config.getString("general.claim-viewer"));
        if(claimViewer == null) {
            log("Invalid material found in config under general.claim-viewer: " +
                    config.getString("general.claim-viewer"));
            claimViewer = Material.STICK;
        }
        claimBlocksGainedPerMinute = (double)config.getInt("general.claim-blocks-gained-per-hour") / 60.0;

        // Register default region flag values
        RegionFlag.registerDefaults(config);
    }
}
