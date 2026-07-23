/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.lower.BridgesSupport
import org.jetbrains.kotlin.backend.konan.lower.CachesAbiSupport
import org.jetbrains.kotlin.backend.konan.lower.EnumsSupport
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable

/**
 * The context type for Native second-stage lowerings.
 *
 * Implemented by both [NativeBackendContext] and (by delegation to it) [NativeGenerationState],
 * so a lowering that needs nothing beyond [CommonBackendContext] plus the Native-specific members
 * declared here should take this type instead of either concrete class.
 */
internal interface NativeLoweringContext : CommonBackendContext, NativeBackendPhaseContext {
    override val symbols: BackendNativeSymbols

    val symbolTable: ReferenceSymbolTable
    val bridgesSupport: BridgesSupport
    val enumsSupport: EnumsSupport
    val cachesAbiSupport: CachesAbiSupport
}
