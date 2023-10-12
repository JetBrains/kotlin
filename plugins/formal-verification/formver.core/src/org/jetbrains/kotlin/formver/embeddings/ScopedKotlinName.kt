/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName

/**
 * Name of a Kotlin entity in the original program in a specified scope and optionally distinguished by type.
 */
data class ScopedKotlinName(val scope: NameScope, val name: KotlinName, val type: TypeEmbedding? = null) : MangledName {
    override val mangled: String
        get() = listOfNotNull(scope.mangled, name.mangled, type?.name?.mangled).joinToString("$")

    val isCollection: Boolean
        get() = if (scope is PackagePrefixScope) {
            scope.packageName.asString() == "kotlin.collections"
        } else {
            false
        }
}

fun FqName.asViperString() = asString().replace('.', '$')