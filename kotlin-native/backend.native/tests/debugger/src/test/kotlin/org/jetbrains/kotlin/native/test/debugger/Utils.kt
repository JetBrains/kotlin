/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.test.debugger

import java.io.IOException

internal fun targetIsHost() = System.getProperty("kotlin.native.host") == System.getProperty("kotlin.native.test.target")

internal fun simulatorTestEnabled() = System.getProperty("kotlin.native.test.debugger.simulator.enabled")?.toBoolean() ?: false

internal fun simulatorDelay() = System.getProperty("kotlin.native.test.debugger.simulator.delay")?.toLongOrNull() ?: 300

internal fun target() = System.getProperty("kotlin.native.test.target")

internal val haveDwarfDump: Boolean by lazy {
    val version = try {
        subprocess(DistProperties.dwarfDump, "--version")
                .takeIf { it.process.exitValue() == 0 }
                ?.stdout
    } catch (e: IOException) {
        null
    }

    if (version == null) {
        println("No LLDB found")
    } else {
        println("Using $version")
    }

    version != null
}

internal val haveLldb: Boolean by lazy {
    val lldbVersion = try {
        subprocess(DistProperties.lldb, "-version")
                .takeIf { it.process.exitValue() == 0 }
                ?.stdout
    } catch (e: IOException) {
        null
    }

    if (lldbVersion == null) {
        println("No LLDB found")
    } else {
        println("Using $lldbVersion")
    }

    lldbVersion != null
}

internal fun lldbCommandRunOrContinue() = if (targetIsHost()) "r" else "c"