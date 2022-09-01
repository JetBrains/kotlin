/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.resolve.BindingContext

internal data class BasicPhaseContextPayload(
        override val config: KonanConfig,
        override val irBuiltIns: IrBuiltIns,
        override val typeSystem: IrTypeSystemContext,
        override val builtIns: KonanBuiltIns,
        override val ir: KonanIr,
        override val lazyValues: MutableMap<LazyMember<*>, Any?> = mutableMapOf()
) : AbstractKonanBackendContext(config) {
    override val configuration: CompilerConfiguration
        get() = config.configuration
}

internal abstract class BasicPhaseContext(
        val payload: BasicPhaseContextPayload,
) : AbstractKonanBackendContext(payload.config), PhaseContext {
    override val irBuiltIns: IrBuiltIns
        get() = payload.irBuiltIns
    override val typeSystem: IrTypeSystemContext
        get() = payload.typeSystem
    override val configuration: CompilerConfiguration
        get() = payload.configuration
    override val lazyValues: MutableMap<LazyMember<*>, Any?>
        get() = payload.lazyValues
    override val builtIns: KonanBuiltIns
        get() = payload.builtIns
    override val ir: KonanIr
        get() = payload.ir

    override val isNativeLibrary: Boolean by lazy {
        val kind = config.configuration.get(KonanConfigKeys.PRODUCE)
        kind == CompilerOutputKind.DYNAMIC || kind == CompilerOutputKind.STATIC
    }
}

internal class FrontendContextImpl(
        config: KonanConfig,
        override var environment: KotlinCoreEnvironment,
) : AbstractKonanBackendContext(config), FrontendContext {
    override lateinit var moduleDescriptor: ModuleDescriptor
    override lateinit var bindingContext: BindingContext
    override lateinit var frontendServices: FrontendServices

    override val irBuiltIns: IrBuiltIns
        get() = TODO("Not yet implemented")
    override val typeSystem: IrTypeSystemContext
        get() = TODO("Not yet implemented")
    override val lazyValues: MutableMap<LazyMember<*>, Any?>
        get() = TODO("Not yet implemented")
    override val builtIns: KonanBuiltIns
        get() = TODO("Not yet implemented")
    override val ir: KonanIr
        get() = TODO("Not yet implemented")
    override val isNativeLibrary: Boolean
        get() = TODO("Not yet implemented")
}

internal class LlvmCodegenContextImpl(
        payload: BasicPhaseContextPayload,
        override val cStubsManager: CStubsManager,
        override val objCExportNamer: ObjCExportNamer?,
        override val llvm: Llvm,
        override val coverage: CoverageManager,
        override var llvmModule: LLVMModuleRef?,
        override val llvmModuleSpecification: LlvmModuleSpecification
) : BasicPhaseContext(payload), LlvmCodegenContext {
    override var bitcodeFileName: String = ""
}

internal class ObjectFilesContextImpl(
        payload: BasicPhaseContextPayload,
) : BasicPhaseContext(payload), ObjectFilesContext {
    override var compilerOutput: List<ObjectFile> = emptyList()
}

internal class LinkerContextImpl(
        payload: BasicPhaseContextPayload,
        override val necessaryLlvmParts: NecessaryLlvmParts,
        override val coverage: CoverageManager,
        override val llvmModuleSpecification: LlvmModuleSpecification,
) : BasicPhaseContext(payload), LinkerContext