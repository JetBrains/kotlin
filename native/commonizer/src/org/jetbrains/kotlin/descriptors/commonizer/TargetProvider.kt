/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

class TargetProvider(
    val target: InputTarget,
    val builtInsClass: Class<out KotlinBuiltIns>,
    val builtInsProvider: BuiltInsProvider,
    val modulesProvider: ModulesProvider
)

interface BuiltInsProvider {
    fun loadBuiltIns(): KotlinBuiltIns

    companion object {
        fun wrap(builtIns: KotlinBuiltIns) = object : BuiltInsProvider {
            override fun loadBuiltIns() = builtIns
        }
    }
}

interface ModulesProvider {
    fun loadModules(): Collection<ModuleDescriptor>

    companion object {
        fun wrap(modules: Collection<ModuleDescriptor>) = object : ModulesProvider {
            override fun loadModules() = modules
        }

        fun wrap(vararg modules: ModuleDescriptor) = wrap(modules.toList())
    }
}
