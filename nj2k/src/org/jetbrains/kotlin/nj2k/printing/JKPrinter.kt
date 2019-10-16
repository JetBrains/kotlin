/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.printing

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.JKElementInfoStorage
import org.jetbrains.kotlin.nj2k.JKImportStorage
import org.jetbrains.kotlin.nj2k.symbols.JKSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal open class JKPrinterBase {
    private val stringBuilder: StringBuilder = StringBuilder()
    var currentIndent = 0;
    private val indentSymbol = " ".repeat(4)
    private var lastSymbolIsLineBreak = false

    override fun toString(): String = stringBuilder.toString()

    fun print(value: String) {
        if (value.isNotEmpty()) {
            lastSymbolIsLineBreak = false
        }
        stringBuilder.append(value)
    }

    fun println() {
        if (lastSymbolIsLineBreak) return
        stringBuilder.append('\n')
        repeat(currentIndent) {
            stringBuilder.append(indentSymbol)
        }
        lastSymbolIsLineBreak = true
    }


    inline fun indented(block: () -> Unit) {
        currentIndent++
        block()
        currentIndent--
    }

    inline fun block(body: () -> Unit) {
        par(ParenthesisKind.CURVED) {
            indented(body)
        }
    }

    inline fun par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
        print(kind.open)
        body()
        print(kind.close)
    }

    inline fun <T> renderList(list: List<T>, separator: String = ", ", renderElement: (T) -> Unit) =
        renderList(list, { this.print(separator) }, renderElement)

    inline fun <T> renderList(list: List<T>, separator: () -> Unit, renderElement: (T) -> Unit) {
        if (list.isEmpty()) return
        renderElement(list.first())
        for (element in list.subList(1, list.size)) {
            separator()
            renderElement(element)
        }
    }

    enum class ParenthesisKind(val open: String, val close: String) {
        ROUND("(", ")"),
        CURVED("{", "}"),
        ANGLE("<", ">")
    }
}

internal class JKPrinter(
    project: Project,
    importStorage: JKImportStorage,
    private val elementInfoStorage: JKElementInfoStorage
) : JKPrinterBase() {
    val symbolRenderer = JKSymbolRenderer(importStorage, project)

    private fun JKType.renderTypeInfo() {
        this@JKPrinter.print(elementInfoStorage.getOrCreateInfoForElement(this).render())
    }

    fun renderType(type: JKType, owner: JKTreeElement?) {
        if (type is JKNoType) return
        if (type is JKCapturedType) {
            when (val wildcard = type.wildcardType) {
                is JKVarianceTypeParameterType -> {
                    renderType(wildcard.boundType, owner)
                }
                is JKStarProjectionType -> {
                    type.renderTypeInfo()
                    this.print("Any?")
                }
            }
            return
        }
        type.renderTypeInfo()
        when (type) {
            is JKClassType -> {
                renderSymbol(type.classReference, owner)
            }
            is JKContextType -> return
            is JKStarProjectionType ->
                this.print("*")
            is JKTypeParameterType ->
                this.print(type.identifier.name)
            is JKVarianceTypeParameterType -> {
                when (type.variance) {
                    JKVarianceTypeParameterType.Variance.IN -> this.print("in ")
                    JKVarianceTypeParameterType.Variance.OUT -> this.print("out ")
                }
                renderType(type.boundType, null)
            }
            else -> this.print("Unit /* TODO: ${type::class} */")
        }
        if (type is JKParametrizedType && type.parameters.isNotEmpty()) {
            par(ParenthesisKind.ANGLE) {
                renderList(type.parameters, renderElement = { renderType(it, null) })
            }
        }
        // we print undefined types as nullable because we need smartcast to work in nullability inference in post-processing
        if (type !is JKWildCardType
            && (type.nullability == Nullability.Default
                    && owner?.safeAs<JKLambdaExpression>()?.functionalType?.type != type
                    || type.nullability == Nullability.Nullable)
        ) {
            this.print("?")
        }
    }

    fun renderSymbol(symbol: JKSymbol, owner: JKTreeElement?) {
        print(symbolRenderer.renderSymbol(symbol, owner))
    }
}