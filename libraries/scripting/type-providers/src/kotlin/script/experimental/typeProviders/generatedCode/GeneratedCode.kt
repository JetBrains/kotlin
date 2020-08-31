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

    companion object {
        /**
         * Create a composition of multiple instances of Generated Code
         */
        operator fun invoke(init: Builder.() -> Unit) = StandardBuilder().apply(init).build()

        /**
         * Create a composition of multiple instances of Generated Code
         */
        operator fun invoke(iterable: Collection<GeneratedCode>): GeneratedCode = this {
            iterable.forEach { code ->
                +code
            }
        }

        /**
         * Create a composition of multiple instances of Generated Code
         */
        operator fun invoke(vararg code: GeneratedCode) = this(code.toList())
    }
}

/**
 * A builder for a composite of multiple types
 */
internal class StandardBuilder internal constructor() : GeneratedCode.Builder {
    private val mutableCodeList = mutableListOf<InternalGeneratedCode>()

    override operator fun GeneratedCode.unaryPlus() {
        when (this) {
            is InternalGeneratedCode -> mutableCodeList.add(this)
            else -> body()
        }
    }

    internal fun build(): GeneratedCode = when (mutableCodeList.count()) {
        0 -> GeneratedCode.Empty
        1 -> mutableCodeList.first()
        else -> CompoundGeneratedCode(mutableCodeList)
    }
}