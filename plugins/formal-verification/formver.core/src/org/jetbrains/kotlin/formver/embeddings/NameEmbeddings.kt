/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * This file contains classes to mangle Kotlin names, such as variables,
 * classes and so on.
 */

/**
 * Representation for Kotlin local variable names.
 */
data class LocalName(val name: Name) : MangledName {
    override val mangled: String
        get() = "local\$${name.asStringStripSpecialMarkers()}"
}

data class ClassName(val packageName: FqName, val className: Name) : MangledName {
    /**
     * Example of mangled class' name:
     * ```kotlin
     * val cn = ClassName("test", "Foo")
     * assert(cf.mangled == "\$pkg_test\$class\$Foo"
     * ```
     */
    override val mangled: String
        get() = "\$pkg_${packageName.asString()}\$class\$${className.asString()}"
}

data class ClassMemberName(val className: ClassName, val name: Name) : MangledName {

    /**
     * Example of mangled class' member name:
     * ```kotlin
     * val cfn = ClassMemberName(ClassName("Foo"), "bar")
     * assert(cfn.mangled == "class\$Foo\$member\$bar")
     * ```
     */
    override val mangled: String
        get() = "${className.mangled}\$member\$${name.asString()}"
}

/**
 * This is a barebones representation of global names.  We'll need to
 * expand it to include classes, but let's keep things simple for now.
 */
data class GlobalName(val packageName: FqName, val name: Name) : MangledName {
    override val mangled: String
        get() = "global\$pkg_${packageName.asString()}\$${name.asStringStripSpecialMarkers()}"
}

fun FirValueParameterSymbol.embedName(): LocalName = LocalName(name)
fun CallableId.embedName(): MangledName = if (isLocal) {
    LocalName(callableName)
} else if (!isLocal && className != null) {
    val name = ClassName(packageName, className!!.shortName())
    ClassMemberName(name, callableName)
} else {
    GlobalName(packageName, callableName)
}
