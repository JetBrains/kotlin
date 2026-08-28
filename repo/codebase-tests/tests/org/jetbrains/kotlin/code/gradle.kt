/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.test.assertTrue

val projectDir = Path("").also { path ->
    assertTrue(path.resolve("settings.gradle.kts").isRegularFile())
}

const val defaultGradleXmx = "2g"

fun defaultGradleArguments(xmx: String = defaultGradleXmx) = buildList {
    add("-Dkotlin.daemon.options=\"autoshutdownIdleSeconds=10\"")
    add("-Dorg.gradle.daemon.idletimeout=1000")
    add("-Dorg.gradle.jvmargs=${defaultGradleJvmArguments(xmx = xmx).joinToString(" ")}")
}

fun defaultGradleJvmArguments(xmx: String = defaultGradleXmx) = buildList {
    add("-Xms256m")
    add("-Xmx${xmx}")

    //progressively take memory, aggressive yielding memory back to the OS
    add("-XX:MinHeapFreeRatio=10")
    add("-XX:MaxHeapFreeRatio=30")
    addAll(issueNewDebugSessionJvmArguments("Gradle(in Test)"))
}
