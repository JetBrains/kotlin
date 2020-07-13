/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

class TargetProvider(
    val target: InputTarget,
    val builtInsClass: Class<out KotlinBuiltIns>,
    val builtInsProvider: BuiltInsProvider,
    val modulesProvider: ModulesProvider
)

interface BuiltInsProvider {
    fun loadBuiltIns(): KotlinBuiltIns

    companion object {
        val defaultBuiltInsProvider = object : BuiltInsProvider {
            override fun loadBuiltIns() = DefaultBuiltIns.Instance
        }
    }
}

interface ModulesProvider {
    class ModuleInfo(val name: String, val originalLocation: File)

    fun loadModuleInfos(): Map<String, ModuleInfo>
    fun loadModules(): Map<String, ModuleDescriptor>
}
