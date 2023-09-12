/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
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

interface FqMangledName : MangledName {
    val packageName: FqName
    val postfix: String

    // We need to replace the dots in the package name as Viper doesn't allow dots in names
    override val mangled: String
        get() = "pkg\$${packageName.asString().replace('.', '$')}\$$postfix"
}

/**
 * Representation for Kotlin local variable names.
 */
data class LocalName(val name: Name) : MangledName {
    override val mangled: String
        get() = "local\$${name.asStringStripSpecialMarkers()}"
}

data class ClassName(override val packageName: FqName, val className: Name) : FqMangledName {
    override val postfix: String = "class_${className.asString()}"
}

data class ClassMemberName(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$member_${name.asString()}"
}

data class ClassMemberGetter(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$getter_${name.asString()}"
}

data class ClassMemberSetter(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$setter_${name.asString()}"
}

/**
 * Global name not associated with a class.
 */
data class GlobalName(override val packageName: FqName, val name: Name) : FqMangledName {
    override val postfix: String = "global$${name.asStringStripSpecialMarkers()}"
}

fun FirValueParameterSymbol.embedName(): LocalName = LocalName(name)

fun FirPropertyAccessorSymbol.embedName(): MangledName {
    val callableId = propertySymbol.callableId
    val name = ClassName(callableId.packageName, callableId.className!!.shortName())
    val propertyName = propertySymbol.callableId.callableName
    return when {
        isGetter -> ClassMemberGetter(name, propertyName)
        isSetter -> ClassMemberSetter(name, propertyName)
        else -> throw IllegalStateException("A property accessor must be a setter or a getter!")
    }
}

fun FirFunctionSymbol<*>.embedName(): MangledName = when (this) {
    is FirPropertyAccessorSymbol -> embedName()
    else -> callableId.embedName()
}

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