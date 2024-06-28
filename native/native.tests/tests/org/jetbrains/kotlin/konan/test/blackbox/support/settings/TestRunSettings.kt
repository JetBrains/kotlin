/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import java.io.File

/**
 * All instances of test classes.
 *
 * [allInstances] - all test class instances ordered from outermost to innermost
 * [enclosingTestInstance] - the outermost test instance
 * [externalSourceTransformersProvider] - transformers provider of the outermost test instance
 */
class NativeTestInstances<T>(val allInstances: List<Any>) {
    @Suppress("UNCHECKED_CAST")
    internal val enclosingTestInstance: T
        get() = allInstances.firstOrNull() as T
    internal val externalSourceTransformersProvider: ExternalSourceTransformersProvider?
        get() = allInstances.firstOrNull() as? ExternalSourceTransformersProvider
}

internal interface ExternalSourceTransformersProvider {
    fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers?
}
