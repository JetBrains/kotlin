/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.handlers.AbstractDescriptorAwareVerifyIdSignaturesByKlib.LoadedModules
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJsResultsConverter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.util.DummyLogger

class JsCollectAndMemorizeIdSignatures(
    testServices: TestServices
) : AbstractCollectAndMemorizeIdSignatures(testServices, JsManglerIr)

class JsVerifyIdSignaturesByDeserializedIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByDeserializedIr(testServices, JsManglerDesc) {
    override fun loadModules(libraries: Collection<KotlinLibrary>) = loadJsModules(testServices, libraries)
    override val createIrLinker get() = ::createJsIrLinker
}

class JsVerifyIdSignaturesByK1LazyIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByK1LazyIr(testServices, JsManglerDesc) {
    override fun loadModules(libraries: Collection<KotlinLibrary>) = loadJsModules(testServices, libraries)
    override val createIrLinker get() = ::createJsIrLinker
}

class JsVerifyIdSignaturesByK2LazyIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByK2LazyIr(testServices) {
    override val fir2IrConverter get() = ::Fir2IrJsResultsConverter
}

private fun loadJsModules(testServices: TestServices, libraries: Collection<KotlinLibrary>): LoadedModules {
    val stdlib = CommonKLibResolver.resolveWithoutDependencies(
        libraries = listOf(testServices.standardLibrariesPathProvider.fullJsStdlib().absolutePath),
        logger = DummyLogger,
        zipAccessor = null
    ).libraries.single()

    val stdlibDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
        stdlib,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        packageAccessHandler = null
    )
    stdlibDescriptor.setDependencies(stdlibDescriptor)

    val libraryDescriptors = libraries.map { library ->
        JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            LanguageVersionSettingsImpl.DEFAULT,
            LockBasedStorageManager.NO_LOCKS,
            stdlibDescriptor.builtIns,
            packageAccessHandler = null,
            LookupTracker.DO_NOTHING
        )
    }

    val allDescriptors = listOf(stdlibDescriptor) + libraryDescriptors
    libraryDescriptors.forEach { libraryDescriptor ->
        libraryDescriptor.setDependencies(allDescriptors)
    }

    return LoadedModules(libraryDescriptors, listOf(stdlibDescriptor))
}

private fun createJsIrLinker(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable, irMessageLogger: IrMessageLogger) =
    JsIrLinker(
        currentModule = null,
        messageLogger = irMessageLogger,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
        translationPluginContext = null,
    )
