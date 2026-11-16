/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.kir.tree.generator.config.AbstractKirTreeBuilderConfigurator

class BuilderConfigurator(model: Model) : AbstractKirTreeBuilderConfigurator(model) {

    override fun configureBuilders() = with(KirTree) {
        val elementsWithBuildCopyFunctions = listOf(constructor, function)

        elementsWithBuildCopyFunctions.forEach {
            builder(it) {
                withCopy()
            }
        }

        configureFieldInAllLeafBuilders("origin") {
            default(it, "KirOrigin.Unknown")
        }

        configureFieldInAllLeafBuilders("visibility") {
            default(it, "KirVisibility.PUBLIC")
        }

        configureFieldInAllLeafBuilders("isOverride") {
            default(it, "false")
        }

        configureFieldInAllLeafBuilders("isInstance") {
            default(it, "true")
        }

        configureFieldInAllLeafBuilders("modality") {
            default(it, "KirModality.UNSPECIFIED")
        }
    }
}
