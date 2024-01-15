/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.test.blackbox.support.CastCompatibleKotlinNativeClassLoader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import kotlin.reflect.KClass


class ClassicFrontend2NativeIrConverter(
    testServices: TestServices,
) : Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.ClassicFrontend,
    BackendKinds.IrBackend
) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider))

    override fun transform(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        return when (module.targetBackend) {
            TargetBackend.NATIVE -> transformToNativeIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend ${module.targetBackend} is not supported for transformation into IR" }
        }
    }

    /**
     * Mostly mimics [org.jetbrains.kotlin.backend.konan.psiToIr], since direct call is impossible due to:
     * - prohibited import of module `:kotlin-native:backend.native` here to `:native:native.tests`
     * - invocation via reflection is complicated due to moving [com.intellij.openapi.project.Project] to another subpackage during compiler
     * JAR embedding.
     *
     * It's unlikely that [org.jetbrains.kotlin.backend.konan.psiToIr] would be ever significantly changed before reaching its end-of-life,
     * so it's plausible to have a reduced copy here in the test pipeline.
     */
    private fun transformToNativeIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val sourceFiles: List<KtFile> = psiFiles.values.toList()
        val translator = Psi2IrTranslator(
            configuration.languageVersionSettings,
            Psi2IrConfiguration(
                ignoreErrors = CodegenTestDirectives.IGNORE_ERRORS in module.directives,
                configuration.partialLinkageConfig.isEnabled
            ),
            configuration.irMessageLogger::checkNoUnboundSymbols
        )
        val manglerDesc = KonanManglerDesc
        val konanIdSignaturerClass = kotlinNativeClass("org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer")
        val konanIdSignaturerConstructor = konanIdSignaturerClass.constructors.single()
        val konanIdSignaturerClassInstance = konanIdSignaturerConstructor.call(manglerDesc) as IdSignatureComposer
        val symbolTable = SymbolTable(konanIdSignaturerClassInstance, IrFactoryImpl)
        val generatorContext = translator.createGeneratorContext(
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            symbolTable
        )

        val konanStubGeneratorExtensionsClass = kotlinNativeClass("org.jetbrains.kotlin.backend.konan.KonanStubGeneratorExtensions")
        val stubGenerator = DeclarationStubGeneratorImpl(
            analysisResult.moduleDescriptor, symbolTable,
            generatorContext.irBuiltIns,
            DescriptorByIdSignatureFinderImpl(analysisResult.moduleDescriptor, manglerDesc),
            konanStubGeneratorExtensionsClass.objectInstance as StubGeneratorExtensions
        ).apply {
            unboundSymbolGeneration = true
        }
        val irDeserializer = object : IrDeserializer {
            override fun getDeclaration(symbol: IrSymbol) = stubGenerator.getDeclaration(symbol)

            override fun resolveBySignatureInModule(
                signature: IdSignature,
                kind: IrDeserializer.TopLevelSymbolKind,
                moduleName: Name
            ): IrSymbol {
                error("Should not be called")
            }

            override fun postProcess(inOrAfterLinkageStep: Boolean) = Unit
        }
        val pluginExtensions = IrGenerationExtension.getInstances(project)

        val moduleFragment = translator.generateModuleFragment(
            generatorContext,
            sourceFiles,
            irProviders = listOf(irDeserializer),
            linkerExtensions = pluginExtensions,
        )

        val pluginContext = IrPluginContextImpl(
            generatorContext.moduleDescriptor,
            generatorContext.bindingContext,
            generatorContext.languageVersionSettings,
            generatorContext.symbolTable,
            generatorContext.typeTranslator,
            generatorContext.irBuiltIns,
            linker = irDeserializer,
            diagnosticReporter = configuration.irMessageLogger
        )

        return IrBackendInput.NativeBackendInput(
            moduleFragment,
            pluginContext,
            diagnosticReporter = DiagnosticReporterFactory.createReporter(),
            descriptorMangler = (pluginContext.symbolTable as SymbolTable).signaturer!!.mangler,
            irMangler = KonanManglerIr,
            firMangler = null,
        )
    }

    private fun kotlinNativeClass(name: String): KClass<out Any> {
        // KT-61248: TODO Replace reflection for direct import, when classes would be extracted out of `backend.native` module
        return Class.forName(
            name,
            true,
            CastCompatibleKotlinNativeClassLoader.kotlinNativeClassLoader.classLoader
        ).kotlin
    }
}
