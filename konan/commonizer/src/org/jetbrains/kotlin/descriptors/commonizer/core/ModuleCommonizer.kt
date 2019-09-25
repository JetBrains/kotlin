/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Module
import org.jetbrains.kotlin.name.Name

interface ModuleCommonizer : Commonizer<Module, Module> {
    companion object {
        fun default(): ModuleCommonizer = DefaultModuleCommonizer()
    }
}

private class DefaultModuleCommonizer : ModuleCommonizer, AbstractStandardCommonizer<Module, Module>() {
    private lateinit var name: Name
    private var konanBuiltIns: KonanBuiltIns? = null

    override fun commonizationResult() = Module(
        name = name,
        builtIns = konanBuiltIns ?: DefaultBuiltIns.Instance
    )

    override fun initialize(first: Module) {
        name = first.name
        konanBuiltIns = first.konanBuiltIns
    }

    override fun doCommonizeWith(next: Module): Boolean {
        // keep the first met KonanBuiltIns when all targets are Kotlin/Native
        // otherwise use DefaultBuiltIns
        if (konanBuiltIns != null) {
            konanBuiltIns = konanBuiltIns.takeIf { next.konanBuiltIns != null }
        }

        return true
    }

    private inline val Module.konanBuiltIns
        get() = builtIns as? KonanBuiltIns
}
