package org.thane.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.UUID;

public class BlockAdapter extends TypeAdapter<Block> {
    @Override
    public void write(JsonWriter out, Block value) throws IOException {
        out.beginObject();
        out.name("world").value(value.getLocation().getWorld().getUID().toString());
        out.name("x").value(value.getX());
        out.name("y").value(value.getY());
        out.name("z").value(value.getZ());
        out.endObject();
    }

    @Override
    public Block read(JsonReader in) throws IOException {
        World world = null;
        Integer x = null;
        Integer y = null;
        Integer z = null;
        in.beginObject();
        while (in.hasNext()) {
            if (in.peek() == JsonToken.NAME) {
                switch (in.nextName()) {
                    case "world":
                        world = Bukkit.getWorld(UUID.fromString(in.nextString()));
                        break;
                    case "x":
                        x = in.nextInt();
                        break;
                    case "y":
                        y = in.nextInt();
                        break;
                    case "z":
                        z = in.nextInt();
                        break;
                }
            }
        }
        in.endObject();
        if (world == null || x == null || y == null || z == null) return null;
        return new Location(world, x,  y, z).getBlock();
    }
}
