/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.symbols.JKSymbol
import org.jetbrains.kotlin.nj2k.symbols.fqNameToImport
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class CollectImportsConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKClassAccessExpression -> addSymbol(element.identifier)
            is JKFieldAccessExpression -> addSymbol(element.identifier)
            is JKMethodCallExpression -> addSymbol(element.identifier)
            is JKAnnotation -> addSymbol(element.classSymbol)
            is JKJavaNewExpression -> addSymbol(element.classSymbol)
            is JKInheritanceInfo -> {
                element.implements
            }
            is JKTypeElement -> {
                element.type.safeAs<JKClassType>()?.also {
                    addSymbol(it.classReference)
                }
            }
        }
        return recurse(element)
    }

    private fun addSymbol(symbol: JKSymbol) {
        symbol.fqNameToImport()?.also { fqName ->
            context.importStorage.addImport(FqName(fqName))
        }
    }
}