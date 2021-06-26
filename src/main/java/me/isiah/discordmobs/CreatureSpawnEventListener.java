package me.isiah.discordmobs;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.EntityType;

public class CreatureSpawnEventListener implements Listener {
    private static final List<EntityType> HOSTILE_MOBS = List.of(EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER,
    EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER,
    EntityType.GHAST, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PIGLIN_BRUTE,
    EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SHULKER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SKELETON_HORSE,
    EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER_SKELETON,
    EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER);

    private static final List<EntityType> PASSIVE_MOBS = List.of(EntityType.AXOLOTL, EntityType.BAT, EntityType.BEE, EntityType.CHICKEN,
    EntityType.COD, EntityType.COW, EntityType.DOLPHIN, EntityType.FOX, EntityType.GLOW_SQUID, EntityType.GOAT, EntityType.MUSHROOM_COW,
    EntityType.OCELOT, EntityType.PANDA, EntityType.PIG, EntityType.PIGLIN, EntityType.POLAR_BEAR, EntityType.PUFFERFISH, EntityType.RABBIT,
    EntityType.SALMON, EntityType.SHEEP, EntityType.SKELETON_HORSE, EntityType.SQUID, EntityType.STRIDER, EntityType.TROPICAL_FISH, EntityType.TURTLE,
    EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.WANDERING_TRADER, EntityType.ZOMBIFIED_PIGLIN);

    private static final List<EntityType> TAMEABLE_MOBS = List.of(EntityType.CAT, EntityType.DONKEY, EntityType.HORSE, EntityType.MULE, EntityType.PARROT, EntityType.WOLF);

    private HashMap<EntityType, Integer> CUSTOM_ENTITIES = new HashMap<EntityType, Integer>();

    private final Random rand = new Random();
    private final Main plugin;

    public CreatureSpawnEventListener(Main plugin) {
        this.plugin = plugin;
    }
    /* TODO: *Add optional death message to names mobs
             * Try to reduce distance of nameplates/ only show if within distance of user(EntityTargetEvent??/ getNearbentite??)*/

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if(this.plugin.discord.isMembersLoaded()) {
            int rand = this.rand.nextInt(100);
            LivingEntity spawnedEntity = event.getEntity();
            if(CUSTOM_ENTITIES.containsKey(spawnedEntity.getType())) {
                if(rand < CUSTOM_ENTITIES.get(spawnedEntity.getType())) {
                    setName(spawnedEntity);
                }
            }
            else {
                if(HOSTILE_MOBS.contains(spawnedEntity.getType()) && rand < this.plugin.config.getInt("hostile_mobs_chance")) {
                    setName(spawnedEntity);
                }
                else if(PASSIVE_MOBS.contains(spawnedEntity.getType()) && rand < this.plugin.config.getInt("passive_mobs_chance")) {
                    setName(spawnedEntity);
                }
                else if(TAMEABLE_MOBS.contains(spawnedEntity.getType()) && rand < this.plugin.config.getInt("tameable_mobs_chance")) {
                    setName(spawnedEntity);
                }
            }
        }
    }
    private void setName(LivingEntity entity) {
        entity.setCustomName(this.plugin.discord.getRandomUsername());
        entity.setCustomNameVisible(true);   
    }
    public void addCustomEntity(String entityName, Integer pct) {
        //TODO: Make sure entity exists
        if(pct != -1){
            CUSTOM_ENTITIES.put(EntityType.valueOf(entityName), pct);
        }
    }
}
