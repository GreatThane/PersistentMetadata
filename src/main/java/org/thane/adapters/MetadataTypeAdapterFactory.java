package org.thane.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import jdk.nashorn.internal.ir.Block;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class MetadataTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> clazz = type.getRawType();

        if (MetadataValue.class.isAssignableFrom(clazz)) {
            return (TypeAdapter<T>) new MetadataValueAdapter(gson);
        } else if (Block.class.isAssignableFrom(clazz)) {
            return (TypeAdapter<T>) new BlockAdapter();
        } else if (Plugin.class.isAssignableFrom(clazz)) {
            return (TypeAdapter<T>) new PluginAdapter();
        } else return null;
    }
}
