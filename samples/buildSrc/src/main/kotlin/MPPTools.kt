/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MPPTools")

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import java.nio.file.Paths

/*
 * This file includes short-cuts that may potentially be implemented in Kotlin MPP Gradle plugin in the future.
 */

// Short-cuts for detecting the host OS.
@get:JvmName("isMacos")
val isMacos by lazy { hostOs == "Mac OS X" }

@get:JvmName("isWindows")
val isWindows by lazy { hostOs.startsWith("Windows") }

@get:JvmName("isLinux")
val isLinux by lazy { hostOs == "Linux" }

// Short-cuts for mostly used paths.
@get:JvmName("mingwPath")
val mingwPath by lazy { System.getenv("MINGW64_DIR") ?: "c:/msys64/mingw64" }

@get:JvmName("kotlinNativeDataPath")
val kotlinNativeDataPath by lazy {
    System.getenv("KONAN_DATA_DIR") ?: Paths.get(userHome, ".konan").toString()
}

// A short-cut for evaluation of the default host Kotlin/Native preset.
@JvmOverloads
fun defaultHostPreset(
    subproject: Project,
    whitelist: List<KotlinTargetPreset<*>> = listOf(subproject.kotlin.presets.macosX64, subproject.kotlin.presets.linuxX64, subproject.kotlin.presets.mingwX64)
): KotlinTargetPreset<*> {

    if (whitelist.isEmpty())
        throw Exception("Preset whitelist must not be empty in Kotlin/Native ${subproject.displayName}.")

    val presetCandidate = when {
        isMacos -> subproject.kotlin.presets.macosX64
        isLinux -> subproject.kotlin.presets.linuxX64
        isWindows -> subproject.kotlin.presets.mingwX64
        else -> null
    }

    return if (presetCandidate != null && presetCandidate in whitelist)
        presetCandidate
    else
        throw Exception("Host OS '$hostOs' is not supported in Kotlin/Native ${subproject.displayName}.")
}

// A short-cut to add a Kotlin/Native run task.
@JvmOverloads
fun createRunTask(
        subproject: Project,
        name: String,
        target: KotlinTarget,
        configureClosure: Closure<Any>? = null
): Task {
    val task = subproject.tasks.create(name, RunKotlinNativeTask::class.java, target)
    task.configure(configureClosure ?: task.emptyConfigureClosure())
    return task
}
