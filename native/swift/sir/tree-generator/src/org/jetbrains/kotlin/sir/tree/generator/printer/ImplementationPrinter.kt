/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.printAcceptChildrenMethod
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printTransformChildrenMethod
import org.jetbrains.kotlin.sir.tree.generator.BASE_PACKAGE
import org.jetbrains.kotlin.sir.tree.generator.elementTransformerType
import org.jetbrains.kotlin.sir.tree.generator.elementVisitorType
import org.jetbrains.kotlin.sir.tree.generator.model.*
import org.jetbrains.kotlin.sir.tree.generator.model.ListField
import org.jetbrains.kotlin.sir.tree.generator.swiftIrImplementationDetailAnnotation
import org.jetbrains.kotlin.utils.SmartPrinter

internal class ImplementationPrinter(printer: SmartPrinter) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {

    companion object {
        private val transformInPlace = ArbitraryImportable("$BASE_PACKAGE.util", "transformInPlace")
    }

    override val implementationOptInAnnotation: ClassRef<*>
        get() = swiftIrImplementationDetailAnnotation

    override val pureAbstractElementType: ClassRef<*>
        get() = org.jetbrains.kotlin.sir.tree.generator.pureAbstractElementType

    override fun makeFieldPrinter(printer: SmartPrinter) = object : AbstractFieldPrinter<Field>(printer) {

        override fun forceMutable(field: Field) = field.isMutable

        override fun actualTypeOfField(field: Field): TypeRefWithNullability {
            if (field is ListField) return StandardTypes.mutableList.withArgs(field.baseType)
            return field.typeRef
        }
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods(implementation: Implementation) {

        if (implementation.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(implementation, elementVisitorType, TypeVariable("R"), override = true)
            printBlock {
                // TODO: This is copy-pasted from the IR generator. Factor this out.
                for (child in implementation.walkableChildren) {
                    print(child.name)
                    if (child.nullable) {
                        print("?")
                    }
                    when (child) {
                        is SimpleField -> println(".accept(visitor, data)")
                        is ListField -> {
                            print(".forEach { it")
                            if (child.baseType.nullable) {
                                print("?")
                            }
                            println(".accept(visitor, data) }")
                        }
                    }
                }
            }
        }

        if (implementation.hasTransformChildrenMethod) {
            printTransformChildrenMethod(implementation, elementTransformerType, StandardTypes.unit, override = true)
            printBlock {
                // TODO: This is copy-pasted from the IR generator. Factor this out.
                for (child in implementation.transformableChildren) {
                    print(child.name)
                    when (child) {
                        is SimpleField -> {
                            print(" = ", child.name)
                            if (child.nullable) {
                                print("?")
                            }
                            print(".transform(transformer, data)")
                            val elementRef = child.typeRef as ElementRef<*>
                            if (!elementRef.element.hasTransformMethod) {
                                print(" as ", elementRef.render())
                            }
                            println()
                        }
                        is ListField -> {
                            addImport(transformInPlace)
                            println(".transformInPlace(transformer, data)")
                        }
                    }
                }
            }
        }
    }
}