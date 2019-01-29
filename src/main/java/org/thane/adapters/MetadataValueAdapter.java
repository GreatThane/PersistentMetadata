package org.thane.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class MetadataValueAdapter extends TypeAdapter<MetadataValue> {
    private Gson gson;

    public MetadataValueAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, MetadataValue value) throws IOException {
        out.beginObject();
        out.name("plugin").value(value.getOwningPlugin().getName());
        out.name("class").value(value.value().getClass().getName());
        out.name("value");
        TypeAdapter adapter = gson.getAdapter(value.value().getClass());
        adapter.write(out, value.value());
        out.endObject();
    }

    @Override
    public MetadataValue read(JsonReader in) throws IOException {
        Plugin plugin = null;
        Object value = null;
        Class<?> clazz = null;
        in.beginObject();
        while (in.hasNext()) {
            if (in.peek() == JsonToken.NAME) {
                switch (in.nextName()) {
                    case "plugin":
                        plugin = Bukkit.getPluginManager().getPlugin(in.nextString());
                        break;
                    case "class":
                        try {
                            clazz = Class.forName(in.nextString());
                        } catch (ClassNotFoundException ignored) {
                        }
                        break;
                    case "value":
                        value = gson.getAdapter(clazz).read(in);
                        break;
                }
            }
        }
        in.endObject();
        if (plugin == null || value == null || clazz == null) return null;
        return new FixedMetadataValue(plugin, value);
    }
}
