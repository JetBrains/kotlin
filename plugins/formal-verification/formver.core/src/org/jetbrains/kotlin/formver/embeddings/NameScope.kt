/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName

sealed interface NameScope : MangledName

interface PackagePrefixScope : NameScope {
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
}

// We use the embedded class name here.  It's not clear whether className.scope can be anything
// but GlobalScope(packageName) here, but this approach makes the embedName implementation make
// more sense.
data class ClassScope(override val packageName: FqName, val className: ScopedKotlinName) : PackagePrefixScope {
    override val suffix = "class_scope_${className.mangled}"
}

data object ParameterScope : NameScope {
    override val mangled = "local"
}

data class LocalScope(val level: Int) : NameScope {
    override val mangled = "local$level"
}
