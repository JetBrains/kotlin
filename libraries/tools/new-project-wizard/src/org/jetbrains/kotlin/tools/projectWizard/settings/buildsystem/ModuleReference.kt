/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.core.parse
import org.jetbrains.kotlin.tools.projectWizard.core.valueParser

sealed class ModuleReference {
    abstract val path: ModulePath

    data class ByPath(override val path: ModulePath) : ModuleReference() {
        companion object {
            val parser = valueParser { value, path ->
                val (modulePath) = ModulePath.parser.parse(this, value, path)
                ByPath(modulePath)
            }
        }

        override fun toString(): String = path.toString()
    }

    data class ByModule(val module: Module) : ModuleReference() {
        override val path: ModulePath get() = module.path
        override fun toString(): String = module.name
    }
}