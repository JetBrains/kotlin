/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal interface LlvmImports {
    fun add(origin: CompiledKlibModuleOrigin, onlyBitcode: Boolean = false)
    fun bitcodeIsUsed(library: KonanLibrary): Boolean
    fun nativeDependenciesAreUsed(library: KonanLibrary): Boolean
}

internal val DeclarationDescriptor.llvmSymbolOrigin: CompiledKlibModuleOrigin
    get() {
        assert(!this.isExpectMember) { this }

        val module = this.module
        val moduleOrigin = module.klibModuleOrigin
        when (moduleOrigin) {
            is CompiledKlibModuleOrigin -> return moduleOrigin
            SyntheticModulesOrigin -> error("Declaration is synthetic and can't be an origin of LLVM symbol:\n${this}")
        }
    }

internal val Context.standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin get() = this.stdlibModule.llvmSymbolOrigin
