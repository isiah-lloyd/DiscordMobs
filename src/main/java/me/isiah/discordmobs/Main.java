package me.isiah.discordmobs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import de.jeff_media.updatechecker.UpdateChecker;
import me.isiah.discordmobs.commands.BaseCommand;

public class Main extends JavaPlugin {

    public final static String PERM_STATUS = "discordmobs.status";
    public final static String PERM_CONNECT = "discordmobs.connect";
    public final static String PERM_SYNC = "discordmobs.sync";
    public final static String PERM_RELOAD = "discordmobs.reload";
    public enum PERM_NODES {
      PERM_STATUS,
      PERM_CONNECT,
      PERM_SYNC,
      PERM_RELOAD;
    }

    public FileConfiguration config;
    public DiscordConnector discord;
    private CreatureSpawnEventListener creatureSpawnEvent;
    private Metrics metrics;
    @Override
    public void onEnable() {
      this.saveDefaultConfig();
      this.getConfig().options().copyDefaults(true);
      config = getConfig();
      discord = new DiscordConnector(this);
      creatureSpawnEvent = new CreatureSpawnEventListener(this);
      setCustomEntities();
      getCommand("discordmobs").setExecutor(new BaseCommand(this));
		  getServer().getPluginManager().registerEvents(creatureSpawnEvent, this);
      metrics = new Metrics(this, 11875);
      UpdateChecker.init(this, 93728)
                   .setDownloadLink(93728)
                   .setChangelogLink("https://www.spigotmc.org/resources/discordmobs.93728/updates")
                   .setNotifyByPermissionOnJoin("discordmobs.status")
                   .checkEveryXHours(24)
                   .setUserAgent("DiscordMobs (plugins@isiah.me)")
                   .checkNow();
      createCustomChats();      
    }
    @Override
    public void onDisable() {
      sConfig();
    }
    public void sConfig() {
      saveConfig();
      try {
        ConfigUpdater.update(this, "config.yml", new File(this.getDataFolder(), "config.yml"), Collections.singletonList("override_mob_chance"));
      }
      catch (IOException e) {
        getLogger().warning("[DiscordMobs] There was an issue using ConfigUpdater\n" + e.toString());
      }
      getLogger().info("[DiscordMobs] Config file saved!");
    }
    public void rConfig() {
      reloadConfig();
      this.config = getConfig();
      setCustomEntities();
      this.discord.convertRoleNamesToIds(null, config.getStringList("roles"));
    }
    public void setCustomEntities() {
      if(config.getConfigurationSection("override_mob_chance") != null) {
        for(String key : config.getConfigurationSection("override_mob_chance").getKeys(false)) {
          creatureSpawnEvent.addCustomEntity(key, config.getInt("override_mob_chance."+key));
        }
      }
    }
    private void createCustomChats() {
      metrics.addCustomChart(new SimplePie("is_connected", () -> {
        if(config.getString("access_token").equals("NO_TOKEN")) return "false";
        else return "true";
      }));
      metrics.addCustomChart(new DrilldownPie("hostile_mobs_chance", () -> {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> entry = new HashMap<>();
        Integer hostileMobChance = config.getInt("hostile_mobs_chance");
        entry.put(String.valueOf(hostileMobChance), 1);
        for(int i=0; i < 100; i += 10) {
          if(hostileMobChance >= i && hostileMobChance <= i+10) {
            String iterString = String.valueOf(i);
            String iterPlus10String = String.valueOf(i+10);
            map.put(iterString + "-" + iterPlus10String, entry);
            break;
          }
        }
        getLogger().info(map.toString());
        return map;
      }));
      metrics.addCustomChart(new DrilldownPie("passive_mobs_chance", () -> {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> entry = new HashMap<>();
        Integer holstileMobChance = config.getInt("passive_mobs_chance");
        entry.put(String.valueOf(holstileMobChance), 1);
        for(int i=0; i < 100; i += 10) {
          if(holstileMobChance >= i && holstileMobChance <= i+10) {
            String iterString = String.valueOf(i);
            String iterPlus10String = String.valueOf(i+10);
            map.put(iterString + "-" + iterPlus10String, entry);
            break;
          }
        }
        getLogger().info(map.toString());
        return map;
      }));
      metrics.addCustomChart(new DrilldownPie("tameable_mobs_chance", () -> {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> entry = new HashMap<>();
        Integer holstileMobChance = config.getInt("tameable_mobs_chance");
        entry.put(String.valueOf(holstileMobChance), 1);
        for(int i=0; i <= 100; i += 10) {
          if((holstileMobChance >=  0 && holstileMobChance <= i+10)) {
            String iterString = String.valueOf(i);
            String iterPlus10String = String.valueOf(i+10);
            map.put(iterString + "-" + iterPlus10String, entry);
            break;
          }
        }
        getLogger().info(map.toString());
        return map;
      }));
      if(config.getConfigurationSection("override_mob_chance") != null) {
        for(String key : config.getConfigurationSection("override_mob_chance").getKeys(false)) {
          Map<String, Map<String, Integer>> map = new HashMap<>();
          Map<String, Integer> entry = new HashMap<>();
          if(config.getInt("override_mob_chance."+key) != -1) {
            metrics.addCustomChart(new DrilldownPie("override_mobs", () -> {
              entry.put(String.valueOf(config.getInt("override_mob_chance."+key)), 1);
              map.put(key, entry);
              return map;
            }));
          }
        }
      }
    }
}
