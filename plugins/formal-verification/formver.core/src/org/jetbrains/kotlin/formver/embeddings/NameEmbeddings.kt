/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/** This file contains classes to mangle names present in the Kotlin source.
 *
 * Name components should be separated by dollar signs.
 * If there is a risk of collision, add a prefix.
 */

/**
 * Representation for Kotlin local variable names.
 */
data class LocalName(val name: Name) : MangledName {
    override val mangled: String
        get() = "local\$${name.asStringStripSpecialMarkers()}"
}

data class ClassName(val packageName: FqName, val className: Name) : MangledName {
    override val mangled: String
        get() = "pkg_${packageName.asString()}\$class_${className.asString()}"
}

data class ClassMemberName(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$member_${name.asString()}"
}

/** Global name not associated with a class.
 */
data class GlobalName(val packageName: FqName, val name: Name) : MangledName {
    override val mangled: String
        get() = "pkg_${packageName.asString()}\$global\$${name.asStringStripSpecialMarkers()}"
}

fun FirValueParameterSymbol.embedName() = LocalName(name)
fun CallableId.embedName(): MangledName = when {
    isLocal -> {
        LocalName(callableName)
    }
    className != null -> {
        // The !! is necessary since className is a property from a different package.
        val name = ClassName(packageName, className!!.shortName())
        ClassMemberName(name, callableName)
    }
    else -> {
        GlobalName(packageName, callableName)
    }
}

fun ClassId.embedName() = ClassName(packageFqName, shortClassName)