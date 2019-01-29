package org.thane;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataStore;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.thane.adapters.MetadataTypeAdapterFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public final class PersistentMetadata extends JavaPlugin implements Listener {

    private static GsonBuilder GSON_BUILDER = new GsonBuilder().registerTypeAdapterFactory(new MetadataTypeAdapterFactory()).disableHtmlEscaping().setPrettyPrinting();
    private static Gson GSON = GSON_BUILDER.create();

    public static Gson getGson() {
        return GSON;
    }

    public static void setGson(Gson gson) {
        PersistentMetadata.GSON = gson;
    }

    public static GsonBuilder getGsonBuilder() {
        return GSON_BUILDER;
    }

    public static void setGsonBuilder(GsonBuilder gsonBuilder) {
        GSON_BUILDER = gsonBuilder;
    }

    public static void initiateGson() {
        GSON = GSON_BUILDER.create();
    }

    private static Field blockDataStoreField;
    private static Field worldDataStoreField;
    private static Field playerDataStoreField;
    private static Field entityDataStoreField;
    private static Field metaMapField;

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("NMSUtils")) {
            GSON_BUILDER = NMSUtils.getBuilder();
            GSON_BUILDER.registerTypeAdapterFactory(new MetadataTypeAdapterFactory());
            NMSUtils.instantiateGson();
            GSON = NMSUtils.getGson();
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
        Set<String> metaNames = getConfig().getStringList("persistent-values").stream().map(String::toLowerCase).collect(Collectors.toSet());
        JsonObject object = new JsonObject();
        File json = new File(getDataFolder(), "metadata.json");
        try {
            if (!json.exists()) {
                json.createNewFile();
            }
            String string = String.join("", Files.readAllLines(json.toPath()));
            JsonElement element = new JsonParser().parse(string);
            object = element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            blockDataStoreField = Bukkit.getWorlds().get(0).getClass().getDeclaredField("blockMetadata");
            blockDataStoreField.setAccessible(true);

            worldDataStoreField = getServer().getClass().getDeclaredField("worldMetadata");
            worldDataStoreField.setAccessible(true);

            playerDataStoreField = getServer().getClass().getDeclaredField("playerMetadata");
            playerDataStoreField.setAccessible(true);

            entityDataStoreField = getServer().getClass().getDeclaredField("entityMetadata");
            entityDataStoreField.setAccessible(true);

            metaMapField = blockDataStoreField.get(Bukkit.getWorlds().get(0)).getClass().getSuperclass().getDeclaredField("metadataMap");
            metaMapField.setAccessible(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        MetadataStore meta;
        Type type = new TypeToken<Map<String, Map<Plugin, MetadataValue>>>() {}.getType();
        try {
            if (object.has("blocks")) {
                JsonObject blocks = object.getAsJsonObject("blocks");

                for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                    meta = (MetadataStore) blockDataStoreField.get(Bukkit.getWorld(UUID.fromString(entry.getKey())));
                    JsonObject mapJson = entry.getValue().getAsJsonObject();
                    removeUnwantedMetadata(metaNames, meta, type, mapJson);
                }
            }
            if (object.has("worlds")) {
                meta = (MetadataStore) worldDataStoreField.get(getServer());
                JsonObject mapJson = object.getAsJsonObject("worlds");
                removeUnwantedMetadata(metaNames, meta, type, mapJson);
            }
            if (object.has("players")) {
                meta = (MetadataStore) playerDataStoreField.get(getServer());
                JsonObject mapJson = object.getAsJsonObject("players");
                removeUnwantedMetadata(metaNames, meta, type, mapJson);
            }
            if (object.has("entities")) {
                meta = (MetadataStore) entityDataStoreField.get(getServer());
                JsonObject mapJson = object.getAsJsonObject("entities");
                removeUnwantedMetadata(metaNames, meta, type, mapJson);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void removeUnwantedMetadata(Set<String> metaNames, MetadataStore meta, Type type, JsonObject mapJson) throws IllegalAccessException {
        Map<String, String> keyMap = mapJson.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getKey().toLowerCase().split(":")[e.getKey().toLowerCase().split(":").length - 1]));
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            if (!metaNames.contains(entry.getValue())) {
                mapJson.remove(entry.getKey());
            }
        }
        metaMapField.set(meta, GSON.fromJson(mapJson, type));
    }

    @SuppressWarnings({"unchecked", "Duplicates"})
    @Override
    public void onDisable() {
        try (PrintWriter writer = new PrintWriter(new File(getDataFolder(), "metadata.json"))) {

            JsonObject object = new JsonObject();
            JsonObject worlds = new JsonObject();
            MetadataStore meta;
            Map<String, Map<Plugin, MetadataValue>> map;
            for (World world : Bukkit.getWorlds()) {
                meta = (MetadataStore) blockDataStoreField.get(world);
                map = (Map<String, Map<Plugin, MetadataValue>>) metaMapField.get(meta);
                worlds.add(world.getUID().toString(), GSON.toJsonTree(map));
            }
            object.add("blocks", worlds);
            meta = (MetadataStore) worldDataStoreField.get(getServer());
            map = (Map<String, Map<Plugin, MetadataValue>>) metaMapField.get(meta);
            object.add("worlds", GSON.toJsonTree(map));

            meta = (MetadataStore) playerDataStoreField.get(getServer());
            map = (Map<String, Map<Plugin, MetadataValue>>) metaMapField.get(meta);
            object.add("players", GSON.toJsonTree(map));

            meta = (MetadataStore) entityDataStoreField.get(getServer());
            map = (Map<String, Map<Plugin, MetadataValue>>) metaMapField.get(meta);
            object.add("entities", GSON.toJsonTree(map));

            writer.write(GSON.toJson(object));

        } catch (FileNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "Duplicates"})
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("meta")) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() == null) {
                event.getPlayer().sendMessage(GSON.toJson(event.getClickedBlock().getMetadata("DIAMOND_PICKAXE")));
            } else event.getClickedBlock().setMetadata(event.getPlayer().getInventory().getItemInMainHand().getType().toString(),
                    new FixedMetadataValue(this, event.getClickedBlock().getLocation().toVector().toBlockVector()));
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {
            event.getRightClicked().setMetadata(event.getPlayer().getInventory().getItemInMainHand().getType().toString(),
                    new FixedMetadataValue(this, event.getRightClicked().getLocation().toVector().toBlockVector()));
        }
    }
}
