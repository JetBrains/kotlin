/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.testbase.TransformationsGeneratorState.changeCounter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.appendText


/**
 * Appends top-level `private val` to the content of the file.
 * Throws SecurityException or IOException, if append failed.
 * Every call to [addPrivateVal] or [addPublicVal] generates a new value name.
 */
fun Path.addPrivateVal(): Path {
    appendText("\nprivate val integerValue${changeCounter.incrementAndGet()} = 24\n")
    return this
}

/**
 * Appends top-level `public val` to the content of the file.
 * Throws SecurityException or IOException, if append failed.
 * Every call to [addPrivateVal] or [addPublicVal] generates a new value name.
 */
fun Path.addPublicVal(): Path {
    appendText("\nval integerValue${changeCounter.incrementAndGet()} = 25\n")
    return this
}

/**
 * Ensures that generated names are unique.
 */
private object TransformationsGeneratorState {
    val changeCounter = AtomicInteger(0)
}
