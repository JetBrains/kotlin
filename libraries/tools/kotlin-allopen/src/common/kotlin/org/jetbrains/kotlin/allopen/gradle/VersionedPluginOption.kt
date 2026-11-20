/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

// Allopen option that only exists in some plugin versions. [since] inclusive, [before] exclusive.
// Null [optionValue] matches any value of [optionKey].
internal enum class VersionedPluginOption(
    private val optionKey: String,
    private val optionValue: String? = null,
    private val since: KotlinToolingVersion? = null,
    private val before: KotlinToolingVersion? = null,
) {
    // Allopen < 2.3.20-Beta2 lacks "jpa" in [org.jetbrains.kotlin.allopen.AllOpenPluginNames.SUPPORTED_PRESETS] (KT-79389):
    // build keeps @Entity final, IDE bundled allopen opens it and hides errors. Drop preset so IDE matches build.
    JPA_PRESET(optionKey = AllOpenGradleSubplugin.PRESET_ARG_NAME, optionValue = "jpa", since = KotlinToolingVersion("2.3.20-Beta2")),
    ;

    fun matches(option: SubpluginOption): Boolean =
        option.key == optionKey && (optionValue == null || option.value == optionValue)

    fun isSupportedBy(compilerVersion: KotlinToolingVersion): Boolean =
        (since == null || compilerVersion >= since) && (before == null || compilerVersion < before)
}

// Unknown or unparsable version keeps all options.
internal fun List<SubpluginOption>.filterSupportedBy(compilerVersion: Provider<String>): List<SubpluginOption> {
    val version = compilerVersion.orNull?.let { runCatching { KotlinToolingVersion(it) }.getOrNull() } ?: return this
    return filter { option -> VersionedPluginOption.entries.none { it.matches(option) && !it.isSupportedBy(version) } }
}
