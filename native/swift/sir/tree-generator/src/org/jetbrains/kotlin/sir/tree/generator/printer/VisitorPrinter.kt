/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.utils.SmartPrinter

internal class VisitorPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>?
        get() = null
}