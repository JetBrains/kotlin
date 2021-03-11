/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.File

object KotlinPluginUtil {
    private class KotlinPluginInfo(val id: PluginId, val version: String, val path: File) {
        constructor(plugin: IdeaPluginDescriptor) : this(plugin.pluginId, plugin.version, plugin.path)
    }

    private val KNOWN_KOTLIN_PLUGIN_IDS = listOf(
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlin.native.clion",
        "org.jetbrains.kotlin.native.appcode"
    )

    private val KOTLIN_PLUGIN_INFO: KotlinPluginInfo by lazy {
        val plugin = PluginManagerCore.getPlugins().firstOrNull { it.pluginId.idString in KNOWN_KOTLIN_PLUGIN_IDS }
            ?: error("Kotlin plugin not found: " + PluginManagerCore.getPlugins().contentToString())

        KotlinPluginInfo(plugin)
    }

    val KOTLIN_PLUGIN_ID: PluginId
        get() = KOTLIN_PLUGIN_INFO.id

    private const val PATCHED_PLUGIN_VERSION_KEY = "kotlin.plugin.version"
    private val PATCHED_PLUGIN_VERSION: String? = System.getProperty(PATCHED_PLUGIN_VERSION_KEY, null)

    @JvmStatic
    fun getPluginVersion(): String {
        if (PATCHED_PLUGIN_VERSION != null) {
            assert(isPatched())
            return PATCHED_PLUGIN_VERSION
        }

        @Suppress("DEPRECATION")
        return getPluginVersionFromIdea()
    }

    @Deprecated("This method returns original plugin version. Please use getPluginVersion() instead.")
    @JvmStatic
    fun getPluginVersionFromIdea(): String {
        return KOTLIN_PLUGIN_INFO.version
    }

    @JvmStatic
    fun isPatched(): Boolean {
        return PATCHED_PLUGIN_VERSION != null
    }

    @JvmStatic
    fun isSnapshotVersion(): Boolean {
        return "@snapshot@" == getPluginVersion()
    }

    fun isDevVersion(): Boolean {
        return getPluginVersion().contains("-dev-")
    }

    fun getPluginPath(): File {
        return KOTLIN_PLUGIN_INFO.path
    }
}
