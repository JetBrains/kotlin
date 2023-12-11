/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.AbstractTransformerVoidPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.sir.tree.generator.elementTransformerType
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.utils.SmartPrinter

internal class TransformerVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractTransformerVoidPrinter<Element, Field>(printer) {

    override val transformerSuperClass: ClassRef<PositionTypeParameterRef>
        get() = elementTransformerType
}