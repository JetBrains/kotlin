/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.jetbrains.kotlin.commonizer.tree.CirTreeModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

internal interface InlineSourceBuilderDelegate : InlineSourceBuilder {
    val inlineSourceBuilder: InlineSourceBuilder

    override fun createCirTree(module: InlineSourceBuilder.Module): CirTreeModule {
        return inlineSourceBuilder.createCirTree(module)
    }

    override fun createModuleDescriptor(module: InlineSourceBuilder.Module): ModuleDescriptor {
        return inlineSourceBuilder.createModuleDescriptor(module)
    }
}
