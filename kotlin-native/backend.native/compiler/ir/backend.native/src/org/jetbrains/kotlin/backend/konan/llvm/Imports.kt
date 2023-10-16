/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.library.metadata.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin

private val ModuleDescriptor.llvmSymbolOrigin: CompiledKlibModuleOrigin
    get() {
        assert(!this.isExpectMember) { this }

        val moduleOrigin = klibModuleOrigin
        when (moduleOrigin) {
            is CompiledKlibModuleOrigin -> return moduleOrigin
            SyntheticModulesOrigin -> error("Declaration is synthetic and can't be an origin of LLVM symbol:\n${this}")
        }
    }

internal val Context.standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin get() = this.stdlibModule.llvmSymbolOrigin

internal val IrPackageFragment.llvmSymbolOrigin
    get() = moduleDescriptor.llvmSymbolOrigin