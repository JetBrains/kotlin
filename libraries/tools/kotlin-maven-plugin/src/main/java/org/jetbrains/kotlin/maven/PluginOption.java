package org.jetbrains.kotlin.maven;

public class PluginOption {
    public final String pluginId;
    public final String key;
    public final String value;

    public PluginOption(String pluginId, String key, String value) {
        this.pluginId = pluginId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "plugin:" + pluginId + ":" + key + "=" + value;
    }
}