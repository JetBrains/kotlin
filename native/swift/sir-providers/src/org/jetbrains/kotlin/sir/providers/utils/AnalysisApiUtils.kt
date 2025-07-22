/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.export.utilities.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

public fun KaSymbolModality.isAbstract(): Boolean = when (this) {
    KaSymbolModality.FINAL, KaSymbolModality.OPEN -> false
    KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> true
}

public val KaDeclarationSymbol.deprecatedAnnotation: Deprecated?
    get() = this.annotations[StandardClassIds.Annotations.Deprecated].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val message = (arguments["message"] as? KaAnnotationValue.ConstantValue?)
            ?.value.toString().removeSurrounding("\"")

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

public val KaDeclarationSymbol.throwsAnnotation: Throws?
    get() = this.annotations[ClassId.topLevel(FqName.fromSegments(listOf("kotlin", "Throws")))].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val classes = (arguments["exceptionClasses"] as? KaAnnotationValue.ArrayValue?)
            ?.values?.filterIsInstance<KaAnnotationValue.ClassLiteralValue>()

        Throws()
    }


@Suppress("OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION")
public val KaDeclarationSymbol.optInAnnotation: RequiresOptIn?
    get() = this.annotations[ClassId.topLevel(FqName("kotlin.RequiresOptIn"))].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val message = (arguments["message"] as? KaAnnotationValue.ConstantValue?)
            ?.value?.toString()?.removeSurrounding("\"") ?: ""

        val level = (arguments["level"] as? KaAnnotationValue.EnumEntryValue?)
            ?.callableId?.let {
                require(it.classId == ClassId.topLevel(FqName("kotlin.RequiresOptIn")).createNestedClassId(Name.identifier("Level")))
                when (it.callableName.identifier) {
                    "WARNING" -> RequiresOptIn.Level.WARNING
                    "ERROR" -> RequiresOptIn.Level.ERROR
                    else -> RequiresOptIn.Level.ERROR
                }
            } ?: RequiresOptIn.Level.ERROR

        RequiresOptIn(message, level)
    }

/**
 * Extracts all opt-in requirements for the given declaration.
 * This includes both direct @RequiresOptIn annotations and indirect opt-in requirements
 * from annotations that themselves require opt-in.
 *
 * @return Set of ClassId representing all opt-in markers required for this declaration
 */
context(session: KaSession)
public val KaDeclarationSymbol.allRequiredOptIns: List<ClassId>
    get() = with(session) {
        buildList {
            (this@allRequiredOptIns as? KaClassSymbol)?.let { getSuperClassSymbolNotAny(it) }?.let {
                addAll(it.allRequiredOptIns)
            }

            this@allRequiredOptIns.containingDeclaration?.allRequiredOptIns?.let { addAll(it) }

            for (annotation in this@allRequiredOptIns.annotations) {
                val annotationClassId = annotation.classId ?: continue

                if (annotationClassId == ClassId.topLevel(FqName("kotlin.RequiresOptIn"))) {
                    (this@allRequiredOptIns as? KaClassLikeSymbol)?.classId?.let { add(it) }
                    continue
                }

                annotation.constructorSymbol?.returnType?.symbol?.let { symbol ->
                    if (symbol.optInAnnotation != null) {
                        symbol.classId?.let { add(it) }
                    }
                }
            }
        }.distinct().sortedBy { it.asFqNameString() }
    }