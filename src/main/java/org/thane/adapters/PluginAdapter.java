package org.thane.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class PluginAdapter extends TypeAdapter<Plugin> {
    @Override
    public void write(JsonWriter out, Plugin value) throws IOException {
        out.value(value.getName());
    }

    @Override
    public Plugin read(JsonReader in) throws IOException {
        return Bukkit.getPluginManager().getPlugin(in.nextString());
    }
}
