/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirModule
import org.jetbrains.kotlin.name.Name

interface ModuleCommonizer : Commonizer<CirModule, CirModule> {
    companion object {
        fun default(): ModuleCommonizer = DefaultModuleCommonizer()
    }
}

private class DefaultModuleCommonizer : ModuleCommonizer, AbstractStandardCommonizer<CirModule, CirModule>() {
    private lateinit var name: Name

    override fun commonizationResult() = CirModule(name = name)

    override fun initialize(first: CirModule) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirModule) = true
}
