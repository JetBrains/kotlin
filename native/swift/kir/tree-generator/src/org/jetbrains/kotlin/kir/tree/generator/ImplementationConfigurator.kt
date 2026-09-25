/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.kir.tree.generator.config.AbstractKirTreeImplementationConfigurator

object ImplementationConfigurator : AbstractKirTreeImplementationConfigurator() {

    override fun configure(model: Model) = with(KirTree) {
        // Declare custom implementation classes, see org.jetbrains.kotlin.fir.tree.generator.ImplementationConfigurator
    }

    override fun configureAllImplementations(model: Model) {
        // Use configureFieldInAllImplementations to customize certain fields in all implementation classes
    }
}
