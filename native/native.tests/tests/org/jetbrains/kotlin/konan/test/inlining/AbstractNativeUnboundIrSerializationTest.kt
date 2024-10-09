/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.inlining

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CommonConfigurationKeys
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
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.test.BoxCodegenWithoutBinarySuppressor
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.junit.jupiter.api.Assumptions.assumeTrue
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
open class AbstractNativeUnboundIrSerializationTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    private val registeredSourceTransformers: MutableMap<File, ReplacingSourceTransformer> = ConcurrentHashMap()

    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ReplacingSourceTransformer) {
        registeredSourceTransformers[getAbsoluteFile(testDataFilePath)] = sourceTransformer
    }

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            artifactKind = BinaryKind.NoArtifact
            dependencyKind = DependencyKind.KLib
        }

        defaultDirectives {
            // Kotlin/Native does not have "minimal" stdlib(like other backends do), so full stdlib is needed to resolve
            // `Any`, `String`, `println`, etc.
            +ConfigurationDirectives.WITH_STDLIB
        }

        configureFirParser(FirParser.LightTree)

        useAfterAnalysisCheckers(::BoxCodegenWithoutBinarySuppressor) // or: BlackBoxCodegenSuppressor
        useConfigurators(
            ::NativeEnvironmentConfigurator,
        )
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )
        useAdditionalService(::LibraryProvider)

        facadeStep(::FirFrontendFacade)
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }
        facadeStep(::Fir2IrNativeResultsConverter)
        facadeStep(::FirNativeKlibSerializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::UnboundIrSerializationHandler)
        }
    }

    override fun runTest(@TestDataFile filePath: String) {
        val sourceTransformer = registeredSourceTransformers[getAbsoluteFile(filePath)]
        if (sourceTransformer != null)
            super.runTest(filePath, sourceTransformer)
        else
            super.runTest(filePath)
    }
}

private class UnboundIrSerializationHandler(testServices: TestServices) : KlibArtifactHandler(testServices) {
    private class InlineFunctionUnderTest(val fullyLinkedFunction: IrSimpleFunction) {
        lateinit var partiallyLinkedFunction: IrSimpleFunction

        lateinit var fullyLinkedFunctionProto: ProtoDeclaration
        lateinit var partiallyLinkedFunctionProto: ProtoDeclaration
    }

    private var functionsUnderTestCounter = 0

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val ir = testServices.dependencyProvider.getArtifact(module, BackendKinds.IrBackend)

        val functionsUnderTest = collectInlineFunctions(ir.irModuleFragment)
        if (functionsUnderTest.isEmpty()) return

        functionsUnderTestCounter += functionsUnderTest.size

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val libraries = (sequenceOf(info) + module.allDependencies.asSequence()
            .map { testServices.dependencyProvider.getTestModule(it.moduleName) }
            .mapNotNull { testServices.dependencyProvider.getArtifactSafe(it, ArtifactKinds.KLib) })
            .map {
                resolveSingleFileKlib(
                    KFile(it.outputFile.absolutePath),
                    configuration.getLogger(treatWarningsAsErrors = true)
                )
            }.toList()

        val deserializer = NonLinkingIrInlineFunctionDeserializer(
            irBuiltIns = ir.irPluginContext.irBuiltIns,
            signatureComputer = PublicIdSignatureComputer(KonanManglerIr),
            libraries = libraries
        )

        for (function in functionsUnderTest) {
            // Make a copy of the original (fully linked) function but without the body to emulate Fir2IrLazy function.
            function.partiallyLinkedFunction = emulateInlineFunctionRepresentedByLazyIr(function.fullyLinkedFunction)

            if (function.partiallyLinkedFunction.isFakeOverride) {
                // If this is a fake override, then it has no body and nothing specifically can be deserialized for the function
                // itself. Instead, we shall deserialize the body of the resolved fake override, which eventually will be copied
                // to the call site during inlining.
                deserializer.deserializeInlineFunction(function.partiallyLinkedFunction.resolveFakeOverrideOrFail())
            } else {
                deserializer.deserializeInlineFunction(function.partiallyLinkedFunction)
            }
        }

        checkFunctionsSerialization(configuration, ir.irPluginContext.irBuiltIns, functionsUnderTest)
    }

    private fun emulateInlineFunctionRepresentedByLazyIr(original: IrSimpleFunction): IrSimpleFunction {
        assertions.assertTrue(original.isInline)

        if (original.isFakeOverride) {
            // This is a fake override inline function. It naturally has no body. But it has inline function(s) among
            // overridden symbols.
            // When this function is inlined, the body of the resolved override will be copied to the call site.
            // At the first phase of compilation, both the inline function itself and all its overrides are
            // represented as Lazy Ir function nodes without bodies. So, to emulate the realistic scenario we have
            // to remove bodies for all overridden functions.
            assertions.assertTrue(original.body == null)

            val originalOverride = original.resolveFakeOverrideOrFail()
            val patchedOverride = emulateInlineFunctionRepresentedByLazyIr(originalOverride)

            return original.deepCopyWithSymbols(initialParent = original.parent).also { copy ->
                copy.overriddenSymbols = copy.overriddenSymbols.map { overriddenSymbol ->
                    if (overriddenSymbol == originalOverride.symbol) patchedOverride.symbol else overriddenSymbol
                }
            }
        } else {
            // Make a bodiless copy.
            val body = original.body
            try {
                original.body = null
                return original.deepCopyWithSymbols(initialParent = original.parent).also { copy ->
                    copy.correspondingPropertySymbol = original.correspondingPropertySymbol
                }
            } finally {
                original.body = body
            }
        }
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

//        for (function in functionsUnderTest) {
//            val fullyLinkedFunctionBytes = function.fullyLinkedFunctionProto.toByteArray()
//            val partiallyLinkedFunctionBytes = function.partiallyLinkedFunctionProto.toByteArray()
//
//            assertions.assertEquals(fullyLinkedFunctionBytes.size, partiallyLinkedFunctionBytes.size) {
//                """
//                    Different byte length for serialized function proto $function
//                    Fully-linked: ${fullyLinkedFunctionBytes.size}
//                    Partially-linked: ${partiallyLinkedFunctionBytes.size}
//                """.trimIndent()
//            }
//
//            assertions.assertTrue(fullyLinkedFunctionBytes.contentEquals(partiallyLinkedFunctionBytes)) {
//                """
//                    Different byte sequence for serialized function proto $function
//                """.trimIndent()
//            }
//        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        // It does not make sense to keep tests without a single inline function as "green", because nothing
        // effectively has been tested there. Neither makes it sense to keep them red. Thus, muted.
        assumeTrue(functionsUnderTestCounter > 0, "No inline functions found for test")
    }

    companion object {
        private fun collectInlineFunctions(irFragment: IrModuleFragment): List<InlineFunctionUnderTest> {
            val result = mutableListOf<InlineFunctionUnderTest>()

            irFragment.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    if (declaration.isInline && !declaration.isEffectivelyPrivate()) {
                        result += InlineFunctionUnderTest(fullyLinkedFunction = declaration)
                    }
                    super.visitSimpleFunction(declaration)
                }
            })

            return result
        }

        private fun createSerializer(configuration: CompilerConfiguration, irBuiltIns: IrBuiltIns): (IrSimpleFunction) -> ProtoDeclaration {
            val languageVersionSettings = configuration.languageVersionSettings

            val serializationSettings = IrSerializationSettings(
                languageVersionSettings = languageVersionSettings,

                /*
                 * Important: Do not recompute a signature for a symbol that already has the signature. Why?
                 *
                 * Normally, symbols coming from the frontend should not have any signatures. And there should not be
                 * any problems with computing signatures for them, as far as their IR is fully linked.
                 *
                 * But for symbols coming from `NonLinkingIrInlineFunctionDeserializer` the IR is unlinked (or partially linked).
                 * Computing signatures for such symbols in 99% cases would result in "X is unbound. Signature: Y" error.
                 * So, for such symbols it's better to take the signature as it is and not try to recompute it. Hopefully,
                 * the signature should already be deserialized together with the symbol.
                 */
                reuseExistingSignaturesForSymbols = true,
            )

            val diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
                DiagnosticReporterFactory.createPendingReporter(configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)),
                languageVersionSettings,
            )

            val moduleSerializer = KonanIrModuleSerializer(serializationSettings, diagnosticReporter, irBuiltIns)

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

            return {
                serializer.inFile(dummyFile) {
                    serializer.serializeDeclaration(it)
                }
            }
        }
    }
}
