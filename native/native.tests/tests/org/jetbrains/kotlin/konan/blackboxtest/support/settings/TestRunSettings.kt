/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

/**
 * All instances of test classes.
 *
 * [allInstances] - all test class instances ordered from innermost to outermost
 * [enclosingTestInstance] - the outermost test instance
 */
internal class TestInstances(val allInstances: List<Any>) {
    val enclosingTestInstance: AbstractNativeBlackBoxTest
        get() = allInstances.firstOrNull().cast()
}

internal interface ExternalSourceTransformersProvider {
    fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers?
}
