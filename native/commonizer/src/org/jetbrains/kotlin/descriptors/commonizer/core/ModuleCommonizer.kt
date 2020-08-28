/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirModule
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirModuleFactory
import org.jetbrains.kotlin.name.Name

class ModuleCommonizer : AbstractStandardCommonizer<CirModule, CirModule>() {
    private lateinit var name: Name

    override fun commonizationResult() = CirModuleFactory.create(name = name)

    override fun initialize(first: CirModule) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirModule) = true
}
