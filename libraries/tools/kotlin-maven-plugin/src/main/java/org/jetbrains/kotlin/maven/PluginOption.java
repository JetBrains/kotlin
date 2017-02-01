package org.jetbrains.kotlin.maven;

public class PluginOption {
    /** The plugin name in Maven, e.g. "all-open" */
    public final String pluginName;

    /** The compiler plugin identifier, e.g. "org.jetbrains.kotlin.allopen" */
    public final String pluginId;

    public final String key;
    public final String value;

    public PluginOption(String pluginName, String pluginId, String key, String value) {
        this.pluginName = pluginName;
        this.pluginId = pluginId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "plugin:" + pluginId + ":" + key + "=" + value;
    }
}