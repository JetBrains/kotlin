/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.sir.tree.generator.model.Implementation
import org.jetbrains.kotlin.sir.tree.generator.model.ListField
import org.jetbrains.kotlin.sir.tree.generator.swiftIrImplementationDetailAnnotation

internal class ImplementationPrinter(
    printer: ImportCollectingPrinter
) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {

    override val implementationOptInAnnotation: ClassRef<*>
        get() = swiftIrImplementationDetailAnnotation

    override fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        org.jetbrains.kotlin.sir.tree.generator.pureAbstractElementType

    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<Field>(printer) {

        override fun forceMutable(field: Field) = field.isMutable

        override fun actualTypeOfField(field: Field): TypeRefWithNullability {
            if (field is ListField) return StandardTypes.mutableList.withArgs(field.baseType)
            return field.typeRef
        }
    }

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        // do nothing
    }
}