/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal class KlibToolIrLinker(
        output: KlibToolOutput,
        module: ModuleDescriptor,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable
) : KotlinIrLinker(module, KlibToolLogger(output), irBuiltIns, symbolTable, exportedDependencies = emptyList()) {
    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
            linker = this,
            symbolTable = symbolTable,
            mangler = KonanManglerIr,
            typeSystem = IrTypeSystemContextImpl(builtIns),
            friendModules = emptyMap(),
            partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
    )

    override val returnUnboundSymbolsIfSignatureNotFound get() = true

    override val translationPluginContext get() = shouldNotBeCalled()

    override fun createModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            klib: KotlinLibrary?,
            strategyResolver: (String) -> DeserializationStrategy
    ): IrModuleDeserializer = KlibToolModuleDeserializer(
            module = moduleDescriptor,
            klib = klib ?: error("Expecting kotlin library for $moduleDescriptor"),
            strategyResolver = strategyResolver
    )

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor) = false

    private inner class KlibToolModuleDeserializer(
            module: ModuleDescriptor,
            klib: KotlinLibrary,
            strategyResolver: (String) -> DeserializationStrategy
    ) : BasicIrModuleDeserializer(
            this,
            module,
            klib,
            strategyResolver,
            klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
    )
}
