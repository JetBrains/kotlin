/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.kir.tree.generator.config.AbstractKirTreeBuilderConfigurator

class BuilderConfigurator(model: Model) : AbstractKirTreeBuilderConfigurator(model) {

    override fun configureBuilders() = with(KirTree) {
        builder(function) {
            withCopy()
        }
    }
}
