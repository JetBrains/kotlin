/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeBuilderConfigurator
import org.jetbrains.kotlin.sir.tree.generator.model.Element

class BuilderConfigurator(elements: List<Element>) : AbstractSwiftIrTreeBuilderConfigurator(elements) {

    override fun configureBuilders() = with(SwiftIrTree) {
        configureAllLeafBuilders {
            withCopy()
        }

        configureFieldInAllLeafBuilders("origin") {
            default(it, "SirOrigin.Unknown")
        }

        configureFieldInAllLeafBuilders("visibility") {
            default(it, "SirVisibility.PUBLIC")
        }

        builder(setter) {
            default("parameterName", "\"newValue\"")
        }
    }
}
