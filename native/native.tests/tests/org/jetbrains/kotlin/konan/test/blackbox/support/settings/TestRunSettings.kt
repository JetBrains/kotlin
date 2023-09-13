/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import java.io.File

/**
 * All instances of test classes.
 *
 * [allInstances] - all test class instances ordered from innermost to outermost
 * [enclosingTestInstance] - the outermost test instance
 */
internal class BlackBoxTestInstances(val allInstances: List<Any>) {
    val enclosingTestInstance: AbstractNativeBlackBoxTest
        get() = allInstances.firstOrNull() as AbstractNativeBlackBoxTest
}

internal interface ExternalSourceTransformersProvider {
    fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers?
}
