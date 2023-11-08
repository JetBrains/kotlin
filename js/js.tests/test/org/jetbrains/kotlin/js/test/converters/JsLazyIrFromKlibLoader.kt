/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.getModuleDescriptorByLibrary
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer.TopLevelSymbolKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.KotlinMangler.DescriptorMangler
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput.LazyIrReconstructedFromKlib
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.util.DummyLogger

@OptIn(ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class, FirIncompatiblePluginAPI::class)
abstract class AbstractLazyIrFromKlibLoader(
    protected val testServices: TestServices
) : AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>() {

    final override val inputKind get() = ArtifactKinds.KLib
    final override val outputKind get() = BackendKinds.DeserializedIrBackend
    final override fun shouldRunAnalysis(module: TestModule) = true

    protected abstract val descriptorMangler: DescriptorMangler
    protected abstract val irMangler: IrMangler

    protected abstract fun loadModules(module: TestModule, library: KotlinLibrary): Pair<ModuleDescriptorImpl, Set<ModuleDescriptor>>
    protected abstract fun bindAllUnboundSymbols(
        dependencies: Collection<ModuleDescriptor>,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        messageLogger: IrMessageLogger,
        stubGenerator: DeclarationStubGenerator,
    )

    final override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): IrBackendInput {
        val library = CommonKLibResolver.resolveWithoutDependencies(
            libraries = listOf(inputArtifact.outputFile.absolutePath),
            logger = DummyLogger,
            zipAccessor = null
        ).libraries.single()

        val (moduleDescriptor, dependencies) = loadModules(module, library)

        val symbolTable = SymbolTable(IdSignatureDescriptor(descriptorMangler), IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(
            symbolTable,
            LanguageVersionSettingsImpl.DEFAULT,
            moduleDescriptor,
            extensions = GeneratorExtensions(),
            allowErrorTypeInAnnotations = true // configuration.skipBodies
        )
        val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

        val stubGenerator = DeclarationStubGeneratorImpl(
            moduleDescriptor,
            symbolTable,
            irBuiltIns,
            DescriptorByIdSignatureFinderImpl(moduleDescriptor, descriptorMangler)
        )

        val topLevelIrDeclarations: List<IrDeclaration> = moduleDescriptor.getAllPackagesFragments().flatMap { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors().map { descriptor ->
                when (descriptor) {
                    is ClassDescriptor -> stubGenerator.generateClassStub(descriptor)
                    is TypeAliasDescriptor -> stubGenerator.generateTypeAliasStub(descriptor)
                    is PropertyDescriptor -> stubGenerator.generatePropertyStub(descriptor)
                    is SimpleFunctionDescriptor -> stubGenerator.generateFunctionStub(descriptor)
                    else -> error("Unexpected descriptor type: ${descriptor::class.java}, $descriptor")
                }
            }
        }

        bindAllUnboundSymbols(
            dependencies,
            irBuiltIns,
            symbolTable,
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module).irMessageLogger,
            stubGenerator
        )

        val topLevelIrDeclarationsByPackages: Map<FqName, List<IrDeclaration>> =
            topLevelIrDeclarations.groupBy { it.getPackageFragment().packageFqName }

        val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)
        topLevelIrDeclarationsByPackages.forEach { (packageFqName, irDeclarations) ->
            irModuleFragment.files += IrFileImpl(
                object : IrFileEntry {
                    override val name = "<fictitious-file-for-package-${packageFqName.asString()}>"
                    override val maxOffset = UNDEFINED_OFFSET

                    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                        SourceRangeInfo(
                            "",
                            UNDEFINED_OFFSET,
                            UNDEFINED_LINE_NUMBER,
                            UNDEFINED_COLUMN_NUMBER,
                            UNDEFINED_OFFSET,
                            UNDEFINED_LINE_NUMBER,
                            UNDEFINED_COLUMN_NUMBER
                        )

                    override fun getLineNumber(offset: Int) = UNDEFINED_LINE_NUMBER
                    override fun getColumnNumber(offset: Int) = UNDEFINED_COLUMN_NUMBER
                    override fun getLineAndColumnNumbers(offset: Int) = LineAndColumn(UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)
                },
                IrFileSymbolImpl(),
                packageFqName,
                irModuleFragment
            ).also { irFile ->
                irFile.declarations += irDeclarations
            }
        }

        return LazyIrReconstructedFromKlib(
            irModuleFragment,
            dependentIrModuleFragments = emptyList(),
            object : IrPluginContext {
                override val languageVersionSettings get() = LanguageVersionSettingsImpl.DEFAULT
                override val afterK2 get() = true
                override val moduleDescriptor get() = moduleDescriptor
                override val bindingContext get() = TODO("Not yet implemented")
                override val symbolTable get() = symbolTable
                override val typeTranslator get() = typeTranslator
                override val symbols get() = TODO("Not yet implemented")
                override val platform get() = TODO("Not yet implemented")
                override val irBuiltIns get() = irBuiltIns
                override val annotationsRegistrar get() = TODO("Not yet implemented")
                override fun createDiagnosticReporter(pluginId: String) = TODO("Not yet implemented")
                override fun referenceClass(fqName: FqName) = TODO("Not yet implemented")
                override fun referenceTypeAlias(fqName: FqName) = TODO("Not yet implemented")
                override fun referenceConstructors(classFqn: FqName) = TODO("Not yet implemented")
                override fun referenceFunctions(fqName: FqName) = TODO("Not yet implemented")
                override fun referenceProperties(fqName: FqName) = TODO("Not yet implemented")
                override fun referenceClass(classId: ClassId) = TODO("Not yet implemented")
                override fun referenceTypeAlias(classId: ClassId) = TODO("Not yet implemented")
                override fun referenceConstructors(classId: ClassId) = TODO("Not yet implemented")
                override fun referenceFunctions(callableId: CallableId) = TODO("Not yet implemented")
                override fun referenceProperties(callableId: CallableId) = TODO("Not yet implemented")
                override fun referenceTopLevel(signature: IdSignature, kind: TopLevelSymbolKind, moduleDescriptor: ModuleDescriptor) = TODO("Not yet implemented")
            },
            SimpleDiagnosticsCollector(),
            descriptorMangler,
            irMangler
        )
    }

    companion object {
        private fun ModuleDescriptorImpl.getAllPackagesFragments(): List<PackageFragmentDescriptor> {
            val result = mutableListOf<PackageFragmentDescriptor>()
            val packageFragmentProvider = packageFragmentProviderForModuleContentWithoutDependencies

            fun getSubPackages(fqName: FqName) {
                result += packageFragmentProvider.packageFragments(fqName)
                val subPackages = packageFragmentProvider.getSubPackagesOf(fqName) { true }
                subPackages.forEach { getSubPackages(it) }
            }

            getSubPackages(FqName.ROOT)
            return result
        }
    }
}

class JsLazyIrFromKlibLoader(testServices: TestServices) : AbstractLazyIrFromKlibLoader(testServices) {
    override val descriptorMangler get() = JsManglerDesc
    override val irMangler get() = JsManglerIr

    override fun loadModules(module: TestModule, library: KotlinLibrary): Pair<ModuleDescriptorImpl, Set<ModuleDescriptor>> {
        val dependencies = JsEnvironmentConfigurator.getAllRecursiveDependenciesFor(module, testServices)
        val dependenciesMap = dependencies.associateBy { it.name.asStringStripSpecialMarkers() }

        val moduleDescriptor = getModuleDescriptorByLibrary(library, dependenciesMap)

        return moduleDescriptor to dependencies
    }

    override fun bindAllUnboundSymbols(
        dependencies: Collection<ModuleDescriptor>,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        messageLogger: IrMessageLogger,
        stubGenerator: DeclarationStubGenerator,
    ) {
        val irLinker = JsIrLinker(
            currentModule = null,
            messageLogger = messageLogger,
            builtIns = irBuiltIns,
            symbolTable = symbolTable,
            partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
            translationPluginContext = null,
            friendModules = emptyMap(),
            stubGenerator = stubGenerator
        )

        dependencies.forEach { dependency ->
            irLinker.deserializeIrModuleHeader(
                dependency,
                dependency.kotlinLibrary,
                deserializationStrategy = { DeserializationStrategy.ONLY_DECLARATION_HEADERS }
            )
        }

        dependencies.forEach { dependency ->
            irLinker.moduleDeserializer(dependency).init()
        }

        ExternalDependenciesGenerator(symbolTable, listOf(irLinker, stubGenerator)).generateUnboundSymbolsAsDependencies()
    }
}
