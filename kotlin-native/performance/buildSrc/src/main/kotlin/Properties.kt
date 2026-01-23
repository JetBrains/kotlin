package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

internal val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

internal val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

// Gradle property to add flags to benchmarks run from command line.
internal val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s".toRegex()).orEmpty()

internal val Project.kotlinVersion: String
    get() = property("kotlinVersion") as String

internal val Project.konanVersion: String?
    get() = findProperty("konanVersion") as String?

val Project.nativeJson: String
    get() = project.property("nativeJson") as String

val Project.buildType: NativeBuildType
    get() = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: NativeBuildType.RELEASE

internal val Project.useCSet: Boolean
    get() = (findProperty("useCSet") as String?).toBoolean()