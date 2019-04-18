/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

/*
 * This file includes internal short-cuts visible only inside of the 'buildSrc' module.
 */

internal val hostOs by lazy { System.getProperty("os.name") }
internal val userHome by lazy { System.getProperty("user.home") }

internal val mingwX64Path by lazy { System.getenv("MINGW64_DIR") ?: "c:/msys64/mingw64" }
internal val mingwX86Path by lazy { System.getenv("MINGW32_DIR") ?: "c:/msys32/mingw32" }


internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.macosX64: KotlinNativeTargetPreset
    get() = getByName(::macosX64.name) as KotlinNativeTargetPreset

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.linuxX64: KotlinNativeTargetPreset
    get() = getByName(::linuxX64.name) as KotlinNativeTargetPreset

internal val NamedDomainObjectCollection<KotlinTargetPreset<*>>.mingwX64: KotlinNativeTargetPreset
    get() = getByName(::mingwX64.name) as KotlinNativeTargetPreset

internal val NamedDomainObjectContainer<out KotlinCompilation<*>>.main: KotlinNativeCompilation
    get() = getByName(::main.name) as KotlinNativeCompilation
