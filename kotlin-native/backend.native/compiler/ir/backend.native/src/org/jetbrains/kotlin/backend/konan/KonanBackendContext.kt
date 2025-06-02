/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.library.KotlinLibrary

interface NativeLoweringContext : LoweringContext {
    override val symbols: KonanSymbols
    val config: KonanConfig
    val typeSystem: IrTypeSystemContext
    fun getExternalPackageFragmentLibrary(packageFragment: IrExternalPackageFragment): KotlinLibrary?
}

internal class NativePreSerializationLoweringContext2(
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        diagnosticReporter: DiagnosticReporter,
        override val config: KonanConfig,
        //private val usedLibraries: Map<ModuleDescriptor, KotlinLibrary>,
        private val usedPackages: Map<IrExternalPackageFragment, KotlinLibrary>,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter), NativeLoweringContext {
    private val konanSymbols = KonanSymbols(this, irBuiltIns, configuration)

    override val symbols = konanSymbols

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KlibSharedVariablesManager(symbols)
    }

    override val typeSystem: IrTypeSystemContext
        get() = IrTypeSystemContextImpl(irBuiltIns)

    override fun getExternalPackageFragmentLibrary(packageFragment: IrExternalPackageFragment) = usedPackages[packageFragment]
}


internal abstract class KonanBackendContext(config: KonanConfig)
    : BasicPhaseContext(config), CommonBackendContext, NativeLoweringContext
{
    abstract val builtIns: KonanBuiltIns

    abstract override val symbols: KonanSymbols

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KlibSharedVariablesManager(symbols)
    }

    override val irFactory: IrFactory = IrFactoryImpl

    override val messageCollector: MessageCollector
        get() = super<BasicPhaseContext>.messageCollector
}
