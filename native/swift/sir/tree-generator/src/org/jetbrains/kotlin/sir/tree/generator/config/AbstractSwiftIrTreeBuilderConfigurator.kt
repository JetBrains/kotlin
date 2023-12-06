/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.config

import org.jetbrains.kotlin.generators.tree.config.AbstractBuilderConfigurator
import org.jetbrains.kotlin.sir.tree.generator.BASE_PACKAGE
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.sir.tree.generator.model.Implementation

abstract class AbstractSwiftIrTreeBuilderConfigurator(
    elements: List<Element>
) : AbstractBuilderConfigurator<Element, Implementation, Field, Field>(elements) {

    override val namePrefix: String
        get() = "Sir"

    override val defaultBuilderPackage: String
        get() = "$BASE_PACKAGE.builder"

    override fun builderFieldFromElementField(elementField: Field) = elementField.copy()
}