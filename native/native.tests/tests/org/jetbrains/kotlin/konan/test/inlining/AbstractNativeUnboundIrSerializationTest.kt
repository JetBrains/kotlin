/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.inlining

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.BoxCodegenWithoutBinarySuppressor
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibBackendFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibArtifactHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.junit.jupiter.api.Assumptions
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * For each KLIB produced as a result of "box" test compilation:
 * 1) deserialize it without linking IR symbols,
 * 2) then serialize back,
 * 3) and compare the result of serialization against the original KLIB.
 */
abstract class AbstractNativeUnboundIrSerializationTest : AbstractKotlinCompilerTest() {
    private val registeredSourceTransformers: MutableMap<File, ReplacingSourceTransformer> = ConcurrentHashMap()

    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ReplacingSourceTransformer) {
        registeredSourceTransformers[getAbsoluteFile(testDataFilePath)] = sourceTransformer
    }

    final override fun TestConfigurationBuilder.configuration() = Unit

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            targetBackend = TargetBackend.NATIVE
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useAdditionalService(::LibraryProvider)

        configureFirParser(FirParser.LightTree)
        facadeStep(::FirFrontendFacade)
        firHandlersStep { commonFirHandlersForCodegenTest() }

        facadeStep(::Fir2IrNativeResultsConverter)

        facadeStep(::FirNativeKlibBackendFacade)
        klibArtifactsHandlersStep { useHandlers(::UnboundIrSerializationHandler) }

        useAfterAnalysisCheckers(::BoxCodegenWithoutBinarySuppressor)
    }

    override fun runTest(@TestDataFile filePath: String) {
        mutePlatformTestIfNecessary(filePath)

        val sourceTransformer = registeredSourceTransformers[getAbsoluteFile(filePath)]
        if (sourceTransformer != null)
            super.runTest(filePath, sourceTransformer)
        else
            super.runTest(filePath)
    }

    private fun mutePlatformTestIfNecessary(filePath: String) {
        if (HostManager.hostIsMac) return

        if (InTextDirectivesUtils.isDirectiveDefined(File(filePath).readText(), WITH_PLATFORM_LIBS.name))
            Assumptions.abort<Nothing>("Unbound IR serialization tests that use platform libs are not supported at non-Mac hosts. Test source: $filePath")
    }
}

class UnboundIrSerializationHandler(testServices: TestServices) : KlibArtifactHandler(testServices) {
    private class InlineFunctionUnderTest(
        val signature: IdSignature,
        val fullyLinkedFunction: IrSimpleFunction
    ) {
        lateinit var partiallyLinkedFunction: IrSimpleFunction

        lateinit var fullyLinkedFunctionProto: ProtoDeclaration
        lateinit var partiallyLinkedFunctionProto: ProtoDeclaration
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val ir = testServices.dependencyProvider.getArtifact(module, BackendKinds.IrBackend)

        val functionsUnderTest = collectInlineFunctions(ir.irModuleFragment)
        if (functionsUnderTest.isEmpty()) return

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val library = resolveSingleFileKlib(
            KFile(info.outputFile.absolutePath),
            configuration.getLogger(treatWarningsAsErrors = true)
        )

        val deserializer = NonLinkingIrInlineFunctionDeserializer(ir.irModuleFragment.irBuiltins, listOf(library))

        for (function in functionsUnderTest) {
            function.partiallyLinkedFunction = deserializer.deserializeInlineFunction(function.signature)
        }

        checkFunctionsSerialization(configuration, ir.irModuleFragment.irBuiltins, functionsUnderTest)
    }

    private fun collectInlineFunctions(irFragment: IrModuleFragment): List<InlineFunctionUnderTest> {
        val signatureComposer = PublicIdSignatureComputer(KonanManglerIr)
        val result = mutableListOf<InlineFunctionUnderTest>()

        irFragment.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if (declaration.isInline) {
                    result += InlineFunctionUnderTest(
                        signature = signatureComposer.computeSignature(declaration),
                        fullyLinkedFunction = declaration
                    )
                }
                super.visitSimpleFunction(declaration)
            }
        })

        return result
    }

    private fun checkFunctionsSerialization(
        configuration: CompilerConfiguration,
        irBuiltIns: IrBuiltIns,
        functionsUnderTest: List<InlineFunctionUnderTest>
    ) {
        run {
            val serializer = createSerializer(configuration, irBuiltIns)
            for (function in functionsUnderTest) {
                function.fullyLinkedFunctionProto = serializer(function.fullyLinkedFunction)
            }
        }

        run {
            val serializer = createSerializer(configuration, irBuiltIns)
            for (function in functionsUnderTest) {
                function.partiallyLinkedFunctionProto = serializer(function.partiallyLinkedFunction)
            }
        }

        for (function in functionsUnderTest) {
            val fullyLinkedFunctionBytes = function.fullyLinkedFunctionProto.toByteArray()
            val partiallyLinkedFunctionBytes = function.partiallyLinkedFunctionProto.toByteArray()

            assertions.assertEquals(fullyLinkedFunctionBytes.size, partiallyLinkedFunctionBytes.size) {
                """
                    Different byte length for serialized function proto ${function.signature.render()}
                    Fully-linked: ${fullyLinkedFunctionBytes.size}
                    Partially-linked: ${partiallyLinkedFunctionBytes.size}
                """.trimIndent()
            }

            assertions.assertTrue(fullyLinkedFunctionBytes.contentEquals(partiallyLinkedFunctionBytes)) {
                """
                    Different byte sequence for serialized function proto ${function.signature.render()}
                """.trimIndent()
            }
        }
    }

    private fun createSerializer(configuration: CompilerConfiguration, irBuiltIns: IrBuiltIns): (IrSimpleFunction) -> ProtoDeclaration {
        val languageVersionSettings = configuration.languageVersionSettings
        val diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            DiagnosticReporterFactory.createPendingReporter(),
            languageVersionSettings,
        )

        val moduleSerializer = KonanIrModuleSerializer(
            diagnosticReporter,
            irBuiltIns,
            CompatibilityMode.CURRENT,
            normalizeAbsolutePaths = false, // unimportant
            sourceBaseDirs = emptyList(),
            languageVersionSettings,
        )

        // Only needed for local signature computation.
        val dummyFile = IrFileImpl(
            fileEntry = object : IrFileEntry {
                override val name: String = "<dummy-file>"
                override val maxOffset get() = shouldNotBeCalled()
                override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo = shouldNotBeCalled()
                override fun getLineNumber(offset: Int): Int = shouldNotBeCalled()
                override fun getColumnNumber(offset: Int): Int = shouldNotBeCalled()
                override fun getLineAndColumnNumbers(offset: Int): LineAndColumn = shouldNotBeCalled()
            },
            symbol = IrFileSymbolImpl(),
            packageFqName = FqName("<dummy-package>")
        )

        val serializer = moduleSerializer.createSerializerForFile(dummyFile)

        return { serializer.serializeDeclaration(it) }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) = Unit
}
