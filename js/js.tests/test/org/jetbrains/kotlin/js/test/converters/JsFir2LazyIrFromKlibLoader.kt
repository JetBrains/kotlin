/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.getModuleDescriptorByLibrary
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer.TopLevelSymbolKind
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.util.DummyLogger

// TODO: 1) deserialize FIR from metadata, 2) build Fir2LazyIr
class JsFir2LazyIrFromKlibLoader(testServices: TestServices) : AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>() {
    override val inputKind: TestArtifactKind<BinaryArtifacts.KLib>
        get() = ArtifactKinds.KLib

    override val outputKind: TestArtifactKind<IrBackendInput>
        get() = BackendKinds.DeserializedIrBackend

    @OptIn(SymbolInternals::class)
    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): IrBackendInput? {
//        val firSymbolProvider = getFirSymbolProvider()
//        val firSymbolNamesProvider = firSymbolProvider.symbolNamesProvider
//        val firClassifierStorage = getFirClassifierStorage()
//
//        firSymbolNamesProvider.getPackageNamesWithTopLevelCallables()?.forEach { packageName ->
//            val packageFqName = FqName(packageName)
//
//            firSymbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)?.forEach { classifierName ->
//                val classId = ClassId(packageFqName, classifierName)
//                when (val firSymbol = firSymbolProvider.getClassLikeSymbolByClassId(classId)) {
//                    is FirClassSymbol -> firClassifierStorage.getOrCreateIrClass(firSymbol)
//                    is FirTypeAliasSymbol -> {
//                        val fir = firSymbol.fir
//                        firClassifierStorage.createAndCacheIrTypeAlias(fir)
//                    }
//                    else -> Unit
//                }
//            }
//
//            firSymbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)?.forEach { callableName ->
//                firSymbolProvider.getTopLevelCallableSymbols(packageFqName, callableName).forEach { callable ->
//                    when (val firDeclaration = callable.fir) {
//                        is FirProperty -> TODO("Fir2IrLazyProperty")
//                        is FirSimpleFunction -> TODO("Fir2IrLazySimpleFunction")
//                        else -> Unit
//                    }
//                }
//            }
//        }

        TODO("Not yet implemented")
    }

    override fun shouldRunAnalysis(module: TestModule) = true

    private fun getFirSymbolProvider(): FirSymbolProvider = TODO()
    private fun getFirClassifierStorage(): Fir2IrClassifierStorage = TODO()
}
