/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.generateJsCode
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

// Transforms klib into js code in script-friendly way
class JsScriptDependencyCompiler(
    private val configuration: CompilerConfiguration,
    private val nameTables: NameTables,
    private val symbolTable: SymbolTable
) {
    fun compile(dependencies: List<ModuleDescriptor>): String {
        val builtIns: KotlinBuiltIns = dependencies.single { it.allDependencyModules.isEmpty() }.builtIns
        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        val messageLogger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None
        val moduleName = Name.special("<script-dependencies>")
        val storageManager = LockBasedStorageManager.NO_LOCKS
        val moduleDescriptor = ModuleDescriptorImpl(moduleName, storageManager, builtIns, null).also {
            it.setDependencies(dependencies.map { d -> d as ModuleDescriptorImpl } + it)
            it.initialize(PackageFragmentProvider.Empty)
        }

        val typeTranslator = TypeTranslatorImpl(symbolTable, symbolTable.signaturer, languageVersionSettings, moduleDescriptor)
        val irBuiltIns = IrBuiltIns(builtIns, typeTranslator, symbolTable)
        val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
        irBuiltIns.functionFactory = functionFactory
        val jsLinker = JsIrLinker(null, messageLogger, irBuiltIns, symbolTable, functionFactory, null)

        val irDependencies = dependencies.map { jsLinker.deserializeFullModule(it, it.kotlinLibrary) }
        val moduleFragment = irDependencies.last()
        val irProviders = listOf(jsLinker)

        jsLinker.init(null, emptyList())

        ExternalDependenciesGenerator(symbolTable, irProviders)
            .generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()

        val backendContext = JsIrBackendContext(
            moduleDescriptor,
            irBuiltIns,
            symbolTable,
            moduleFragment,
            emptySet(),
            configuration,
            true
        )

        ExternalDependenciesGenerator(symbolTable, irProviders)
            .generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()
        jsLinker.postProcess()

        moduleFragment.files += irDependencies.filter { it !== moduleFragment }.flatMap { it.files }

        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        return generateJsCode(backendContext, moduleFragment, nameTables)
    }
}
