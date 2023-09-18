/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * This file contains classes to mangle names present in the Kotlin source.
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
    override val postfix: String = "class_${className.asStringStripSpecialMarkers()}"
}

data class ClassConstructorName(val className: ClassName, val paramsTypeSignature: String) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$constructor\$${paramsTypeSignature}"
}

data class ClassFunctionName(val className: ClassName, val functionName: Name, val paramsTypeSignature: String) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$fun_${functionName.asStringStripSpecialMarkers()}\$${paramsTypeSignature}"
}

data class ClassMemberName(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$member_${name.asStringStripSpecialMarkers()}"
}

data class ClassMemberGetter(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$getter_${name.asStringStripSpecialMarkers()}"
}

data class ClassMemberSetter(val className: ClassName, val name: Name) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$setter_${name.asStringStripSpecialMarkers()}"
}

/**
 * Global name not associated with a class.
 */
data class GlobalName(override val packageName: FqName, val name: Name) : FqMangledName {
    override val postfix: String = "global$${name.asStringStripSpecialMarkers()}"
}

data class GlobalFunctionName(override val packageName: FqName, val name: Name, val typeSignature: String) : FqMangledName {
    override val postfix: String = "global$${name.asStringStripSpecialMarkers()}$${typeSignature}"
}

/**
 * Mangle a type's name into an identifier supported by Viper.
 * When the type is Nullable, its name will be prefixed by `NT` (Nullable Type).
 * Otherwise, the prefix will be `T` (Type).
 */
val ConeKotlinType.mangledName: String
    get() {
        val prefix = when (this.isNullable) {
            true -> "NT" // Nullable Type
            false -> "T" // Type
        }
        // Check if there are any type argument in type's name
        val mangled = toString()
            .replace("/", "_")
            .replace(Regex("[? ]"), "")
            .replace(Regex("[<>]"), "__")
            .replace(",", "$")
        return prefix + mangled
    }

/**
 * Create a `String` embedding the type names, joined together by a separator character (`$`).
 * This function is useful to handle cases with function overloading.
 */
fun FirFunctionSymbol<*>.embedTypeSignature(): String = valueParameterSymbols.joinToString("$") { param ->
    param.resolvedReturnType.mangledName
}

fun FirValueParameterSymbol.embedName(): LocalName = LocalName(name)

fun FirPropertyAccessorSymbol.embedName(): MangledName {
    val className = propertySymbol.callableId.classId!!.embedName()
    val propertyName = propertySymbol.callableId.callableName
    return when {
        isGetter -> ClassMemberGetter(className, propertyName)
        isSetter -> ClassMemberSetter(className, propertyName)
        else -> throw IllegalStateException("A property accessor must be a setter or a getter!")
    }
}

fun FirFunctionSymbol<*>.embedName(): MangledName = when (this) {
    is FirPropertyAccessorSymbol -> embedName()
    is FirConstructorSymbol -> ClassConstructorName(callableId.classId!!.embedName(), embedTypeSignature())
    else -> {
        // When mangling the function name we have to take into account function overloading.
        // To distinguish functions with the same name we use function's type signature.
        // Both local/classes functions must be covered.
        when {
            callableId.className != null -> ClassFunctionName(
                callableId.classId!!.embedName(),
                callableId.callableName,
                embedTypeSignature()
            )
            else -> GlobalFunctionName(callableId.packageName, callableId.callableName, embedTypeSignature())
        }
    }
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