package me.isiah.discordmobs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
      setCustomEntities();
    }
    public void setCustomEntities() {
      if(config.getConfigurationSection("override_mob_chance") != null) {
        for(String key : config.getConfigurationSection("override_mob_chance").getKeys(false)) {
          creatureSpawnEvent.addCustomEntity(key, config.getInt("override_mob_chance."+key));
        }
      }
    }
}
