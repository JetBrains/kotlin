/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

public fun KaSymbolModality.isAbstract(): Boolean = when (this) {
    KaSymbolModality.FINAL, KaSymbolModality.OPEN -> false
    KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> true
}

public val KaDeclarationSymbol.deprecatedAnnotation: Deprecated?
    get() = this.annotations.find { it.classId == ClassId.topLevel(StandardNames.FqNames.deprecated) }?.let {
        val message = it.arguments.find { it.name.asString() == "message" }!!
            .let { it.expression as KaAnnotationValue.ConstantValue }
            .value.toString().removeSurrounding("\"")

        val level = it.arguments.find { it.name.asString() == "level" }
            ?.let { it.expression as KaAnnotationValue.EnumEntryValue }
            ?.callableId?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.deprecationLevel))
                runCatching { kotlin.DeprecationLevel.valueOf(it.callableName.identifier) }.getOrNull()
            } ?: DeprecationLevel.WARNING

        val replaceWith = it.arguments.find { it.name.asString() == "replaceWith" }
            ?.let { it.expression as KaAnnotationValue.NestedAnnotationValue }
            ?.annotation?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.replaceWith))
                it.arguments.find { it.name.asString() == "expression" }
                    ?.let { it.expression as KaAnnotationValue.ConstantValue }
                    ?.value.toString().removeSurrounding("\"")
            } ?: ""

        Deprecated(message, level = level, replaceWith = ReplaceWith(replaceWith))
    }