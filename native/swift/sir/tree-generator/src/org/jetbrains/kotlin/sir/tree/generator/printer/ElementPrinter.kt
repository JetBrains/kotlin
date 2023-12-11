/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.sir.tree.generator.*
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.utils.SmartPrinter

internal class ElementPrinter(printer: SmartPrinter) : AbstractElementPrinter<Element, Field>(printer) {

    override fun makeFieldPrinter(printer: SmartPrinter) = object : AbstractFieldPrinter<Field>(printer) {
        override fun forceMutable(field: Field): Boolean = field.isMutable
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods(element: Element) {
        val treeName = "Swift IR"
        if (element.isRootElement || element.parentInVisitor != null) {
            printAcceptMethod(element, elementVisitorType, hasImplementation = !element.isRootElement, treeName)
            printTransformMethod(
                element,
                elementTransformerType,
                implementation = "transformer.transform${element.name}(this, data)",
                returnType = TypeVariable("E", listOf(SwiftIrTree.rootElement)),
                treeName
            )
        }

        if (element.isRootElement) {
            println()
            printAcceptVoidMethod(elementVisitorVoidType, treeName)
            printAcceptChildrenMethod(element, elementVisitorType, TypeVariable("R"))
            println()
            println()
            printAcceptChildrenVoidMethod(elementVisitorVoidType)
            println()
            printTransformVoidMethod(element, elementTransformerVoidType, treeName)
            printTransformChildrenMethod(element, elementTransformerType, StandardTypes.unit)
            println()
            println()
            printTransformChildrenVoidMethod(element, elementTransformerVoidType, StandardTypes.unit)
        }
    }
}