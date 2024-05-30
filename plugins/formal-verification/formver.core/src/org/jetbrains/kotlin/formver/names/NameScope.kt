/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName

sealed interface NameScope : MangledName

sealed interface PackagePrefixScope : NameScope {
    val packageName: FqName
    val suffix: String
    override val mangled: String
        get() = if (packageName.isRoot) {
            suffix
        } else {
            "pkg\$${packageName.asViperString()}\$$suffix"
        }
}

data class GlobalScope(override val packageName: FqName) : PackagePrefixScope {
    override val suffix = "global"

    constructor(segments: List<String>) : this(FqName.fromSegments(segments))
}

sealed interface ClassScope : PackagePrefixScope {
    val className: ClassKotlinName
}

data class DefaultClassScope(override val packageName: FqName, override val className: ClassKotlinName, ) : ClassScope {
    override val suffix = className.mangled
}

/**
 * We do not want to mangle field names with class and package, hence introducing
 * this special `NameScope`. Note that it still needs package and class for other purposes.
 */
data class PublicClassScope(override val packageName: FqName, override val className: ClassKotlinName) : ClassScope {
    override val suffix = className.mangled + "_public"
    override val mangled = "public"
}

data class PrivateClassScope(override val packageName: FqName, override val className: ClassKotlinName) : ClassScope {
    override val suffix = className.mangled + "_private"
}

data object ParameterScope : NameScope {
    override val mangled = "local"
}

data class LocalScope(val level: Int) : NameScope {
    override val mangled = "local$level"
}
