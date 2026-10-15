/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import java.io.Serializable

internal data class DefaultKotlinNpmDependency(
    override val name: String,
    override val version: String,
    override val scope: KotlinNpmDependency.Scope,
) : KotlinNpmDependency, Serializable {
    override fun toString(): String = "$name@$version"
}
