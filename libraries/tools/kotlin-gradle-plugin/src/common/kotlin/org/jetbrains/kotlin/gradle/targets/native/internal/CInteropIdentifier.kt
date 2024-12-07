/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import java.io.Serializable

/**
 * Project unique identifier for all cinterops
 */
internal data class CInteropIdentifier internal constructor(
    val scope: Scope,
    val interopName: String
) : Serializable {
    class Scope @UnsafeApi internal constructor(val name: String) : Serializable {
        companion object {
            @OptIn(UnsafeApi::class)
            fun create(compilation: KotlinCompilation<*>): Scope {
                return Scope("compilation/${compilation.compileKotlinTaskName}")
            }
        }

        override fun toString(): String = name
        override fun hashCode(): Int = name.hashCode()
        override fun equals(other: Any?): Boolean {
            if (other !is Scope) return false
            return this.name == other.name
        }
    }

    @get:Input
    val uniqueName: String = "cinterop:${scope.name}/$interopName"
    override fun toString(): String = uniqueName
}
