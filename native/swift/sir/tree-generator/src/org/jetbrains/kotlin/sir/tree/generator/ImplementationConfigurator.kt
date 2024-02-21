/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeImplementationConfigurator

object ImplementationConfigurator : AbstractSwiftIrTreeImplementationConfigurator() {

    override fun configure(model: Model) = with(SwiftIrTree) {
        // Declare custom implementation classes, see org.jetbrains.kotlin.fir.tree.generator.ImplementationConfigurator
    }

    override fun configureAllImplementations(model: Model) {
        // Use configureFieldInAllImplementations to customize certain fields in all implementation classes
        configureFieldInAllImplementations(
            fieldName = "parent",
        ) {
            isMutable(it)
            isLateinit(it)
        }
    }
}