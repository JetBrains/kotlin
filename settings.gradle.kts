// NOTE: This settings file is completely ignored when running composite build `kotlin` + `kotlin-ultimate`.

import java.lang.Boolean.parseBoolean

val cacheRedirectorEnabled: String? by settings

pluginManagement {
    repositories {
        if (parseBoolean(cacheRedirectorEnabled)) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        }
        gradlePluginPortal()
    }
}

include(":prepare:cidr-plugin",
        ":prepare:appcode-plugin",
        ":prepare:clion-plugin",
        ":ide:cidr-native",
        ":ide:appcode-native",
        ":ide:clion-native"
)