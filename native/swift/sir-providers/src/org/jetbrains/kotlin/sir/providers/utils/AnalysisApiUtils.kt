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
import org.jetbrains.kotlin.name.StandardClassIds

public fun KaSymbolModality.isAbstract(): Boolean = when (this) {
    KaSymbolModality.FINAL, KaSymbolModality.OPEN -> false
    KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> true
}

public val KaDeclarationSymbol.deprecatedAnnotation: Deprecated?
    get() = this.annotations[StandardClassIds.Annotations.Deprecated].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val message = (arguments["message"] as? KaAnnotationValue.ConstantValue?)
            ?.value.toString().removeSurrounding("\"") ?: ""

        val level = (arguments["level"] as? KaAnnotationValue.EnumEntryValue?)
            ?.callableId?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.deprecationLevel))
                runCatching { kotlin.DeprecationLevel.valueOf(it.callableName.identifier) }.getOrNull()
            } ?: DeprecationLevel.WARNING

        val replaceWith = (arguments["replaceWith"] as? KaAnnotationValue.NestedAnnotationValue?)
            ?.annotation?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.replaceWith))
                it.arguments.find { it.name.asString() == "expression" }
                    ?.let { it.expression as KaAnnotationValue.ConstantValue }
                    ?.let { it.value.value as? String }
            } ?: ""

        Deprecated(message, level = level, replaceWith = ReplaceWith(replaceWith))
    }