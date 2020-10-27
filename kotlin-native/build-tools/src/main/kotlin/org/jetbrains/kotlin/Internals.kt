/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File

/*
 * This file includes internal short-cuts visible only inside of the 'buildSrc' module.
 */

internal val hostOs by lazy { System.getProperty("os.name") }
internal val userHome by lazy { System.getProperty("user.home") }

internal val Project.ext: ExtraPropertiesExtension
    get() = extensions.getByName("ext") as ExtraPropertiesExtension

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.macosX64: KotlinTargetPreset<*>
    get() = getByName(::macosX64.name) as KotlinTargetPreset<*>

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.linuxX64: KotlinTargetPreset<*>
    get() = getByName(::linuxX64.name) as KotlinTargetPreset<*>

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.mingwX64: KotlinTargetPreset<*>
    get() = getByName(::mingwX64.name) as KotlinTargetPreset<*>

internal val NamedDomainObjectContainer<out KotlinCompilation<*>>.main: KotlinNativeCompilation
    get() = getByName(::main.name) as KotlinNativeCompilation

internal val FileCollection.isNotEmpty: Boolean
    get() = !isEmpty

internal fun Provider<File>.resolve(child: String): Provider<File> = map { it.resolve(child) }