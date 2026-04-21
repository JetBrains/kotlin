/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.providers.SirDeclarationNamer
import org.jetbrains.kotlin.sir.providers.utils.objCNameAnnotation
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

public class SirDeclarationNamerImpl : SirDeclarationNamer {

    override fun KaDeclarationSymbol.sirDeclarationName(): String {
        return getName() ?: error("could not retrieve a name for $this")
    }

    private fun KaDeclarationSymbol.getName(): String? {
        objCNameAnnotation?.name?.let { return it }
        return when (this) {
            is KaNamedClassSymbol -> this.classId?.shortClassName?.asString()
            is KaPropertySymbol -> this.name.asString()
            is KaCallableSymbol -> this.mangleCallableName()
            else -> error(this)
        }
    }

    private fun KaCallableSymbol.mangleCallableName(): String? {
        this.mangleFactoryNameClashingWithClassLikeSymbol()?.let { return it }

        return callableId?.callableName?.asString()
    }

    /**
     * Kotlin allows having class and function with the same name in the same scope
     * (unless there is a constructor with the same signature as the function).
     * Swift doesn't allow that.
     *
     * Luckily, if Kotlin code follows the naming conventions, there is only one case when
     * such a clash might happen -- factory functions:
     * https://kotlinlang.org/docs/coding-conventions.html#function-names
     * (Otherwise function names should start with a lowercase letter, while class names
     * start with an uppercase).
     *
     * The code below targets specifically this case by detecting such clashing factory functions
     * and lowercasing their Swift names.
     * It deliberately ignores clashes that might occur because of the renaming.
     *
     * See also [KT-70063](https://youtrack.jetbrains.com/issue/KT-70063).
     *
     * @return mangled name, if function is detected to be such a factory; `null` otherwise.
     */
    private fun KaCallableSymbol.mangleFactoryNameClashingWithClassLikeSymbol(): String? {
        if (this !is KaNamedFunctionSymbol || !this.isTopLevel) return null
        val callableId = this.callableId ?: return null
        if (!callableId.callableName.asString().first().isUpperCase()) return null
        // It is a top-level function, and its name starts with an uppercase.

        val returnType = this.returnType.abbreviation ?: this.returnType
        val classIds = generateSequence(returnType.symbol) { symbol ->
            when (symbol) {
                is KaTypeAliasSymbol -> symbol.expandedType.symbol
                is KaClassSymbol -> {
                    val classKind = symbol.classKind.takeIf { it == KaClassKind.INTERFACE } ?: KaClassKind.CLASS
                    symbol.superTypes.filterIsInstanceAnd<KaClassType> {
                        var superSymbol: KaClassLikeSymbol? = it.symbol
                        while (superSymbol is KaTypeAliasSymbol) {
                            superSymbol = superSymbol.expandedType.symbol
                        }
                        (superSymbol as? KaClassSymbol)?.classKind == classKind
                    }.firstOrNull()?.symbol as KaClassSymbol?
                }
            }
        }.takeWhile { it.isTopLevel }.mapNotNull { it.classId }
        // The return type is based on a top-level class or typealias with `classId`.

        return if (classIds.any { callableId.packageName == it.packageFqName && callableId.callableName == it.shortClassName }) {
            // They match ⇒ mangle the function name by lowercasing the first latter.
            callableId.callableName.asString().replaceFirstChar { it.lowercase() }
        } else {
            null
        }
    }
}
