/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeBuilderConfigurator
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Implementation

class BuilderConfigurator(elements: List<Element>) : AbstractSwiftIrTreeBuilderConfigurator(elements) {

    fun configure() = with(SwiftIrTree) {
        // Use builder configurator DSL for fine-tuning the builder generation logic.
        // See org.jetbrains.kotlin.fir.tree.generator.BuilderConfigurator for example usage

        configureFieldInAllLeafBuilders("origin") {
            default(it, "SirOrigin.Unknown")
        }

        configureFieldInAllLeafBuilders("visibility") {
            default(it, "SirVisibility.PUBLIC")
        }

        configureAllLeafBuilders {
            withCopy()
        }
    }
}

//AbstractBuilderConfigurator<Element, Implementation, Field, Field>
private fun <Element, Implementation, BuilderField, ElementField> AbstractBuilderConfigurator<Element, Implementation, BuilderField, ElementField>.configureAllLeafBuilders(
    builderPredicate: (LeafBuilder<BuilderField, Element, Implementation>) -> Boolean = { true },
    init: AbstractBuilderConfigurator<Element, Implementation, BuilderField, ElementField>.LeafBuilderConfigurationContext.() -> Unit,
) where Element : AbstractElement<Element, ElementField, Implementation>,
        Implementation : AbstractImplementation<Implementation, Element, BuilderField>,
        BuilderField : AbstractField<*>,
        BuilderField : AbstractFieldWithDefaultValue<*>,
        ElementField : AbstractField<ElementField> = elements
    .flatMap { it.allImplementations }
    .mapNotNull { it.builder }
    .filter(builderPredicate)
    .forEach { LeafBuilderConfigurationContext(it).init() }