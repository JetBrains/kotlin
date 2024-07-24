/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName

/**
 * Name of a Kotlin entity in the original program in a specified scope and optionally distinguished by type.
 */
data class ScopedKotlinName(val scope: NameScope, val name: KotlinName) : MangledName {
    override val mangledScope: String?
        get() = scope.fullMangledName
    override val mangledBaseName: String
        get() = name.mangledBaseName
    override val mangledType: String?
        get() = name.mangledType
}

fun FqName.asViperString() = asString().replace('.', '_')

fun ScopedKotlinName.asScope(): NameScope {
    val className = name as? ClassKotlinName
    require(className != null) { "Only classes can be used for scopes." }
    return ClassScope(scope, className)
}