/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import java.io.File
import kotlin.script.experimental.typeProviders.generatedCode.impl.*
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode

@DslMarker
annotation class GeneratedCodeDSL

/**
 * Code returned by a Type Provider.
 * [GeneratedCode] is a recursive construction that allows you to build generated code as a combination of multiple types already provided
 *
 * Example:
 *
 * ```kotlin
 * data class MyObject(val name: String, val value: Int) : GeneratedCode {
 *     fun GeneratedCode.Builder.body(): GeneratedCode {
 *         dataClass(name) {
 *             implee
 *         }
 *          return GeneratedCode.composite {
 *              +makeDataClass(name)
 *                  .withInterface(MyInterface::class)
 *
 *              +makeConstant("$nameConstant", value)
 *          }
 *     }
 * }
 * ```
 *
 * [GeneratedCode] can contain any of the following:
 * - Data classes. See [GeneratedCode.Builder.dataClass].
 * - Objects. See [GeneratedCode.Builder.object].
 * - Constants. See [GeneratedCode.Builder.constant].
 * - Extension methods. See [GeneratedCode.Builder.extensionMethod].
 * - etc.
 */
interface GeneratedCode {
    /**
     * Generate the code that corresponds to your type
     *
     * @return An instance of [GeneratedCode].
     * Important: don't return an infinite recursion in the form of return `this`
     */
    fun Builder.body()

    @GeneratedCodeDSL
    interface Builder {
        operator fun GeneratedCode.unaryPlus()
    }

    /**
     * No Code
     */
    object Empty : GeneratedCode {
        override fun Builder.body() {
            // No-op
        }
    }

}

