/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.inlining

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer.XStatementOrExpression
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.serialization.KonanDeclarationTable
import org.jetbrains.kotlin.backend.konan.serialization.KonanGlobalDeclarationTable
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrFileSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.protobuf.MessageLite as ProtoMessage

/**
 * The idea of the test is the following:
 * 1. Take a test data file which has inline function declarations.
 * 2. Compile and serialize the test to KLIB(s).
 * 3. For each inline function: Serialize it and memoize proto, signatures, strings, etc.
 * 4. Also, for each inline function:
 *    1. Erase the body.
 *    2. Then use [NonLinkingIrInlineFunctionDeserializer] to deserialize the body back.
 *    3. Then, serialize the function with the deserialized body once again and memoize proto, signatures, strings, etc.
 * 5. Compare the results of points 3 and 4. They should match.
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
            artifactKind = ArtifactKind.NoArtifact
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
    private data class InlineFunctionUnderTest(val fullyLinkedIrFunction: IrSimpleFunction) {
        lateinit var partiallyLinkedIrFunction: IrSimpleFunction

        lateinit var fullyLinkedSerializedFunction: SerializedFunction
        lateinit var partiallyLinkedSerializedFunction: SerializedFunction
    }

    private var functionsUnderTestCounter = 0

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        if (KlibBasedCompilerTestDirectives.SKIP_UNBOUND_IR_SERIALIZATION in module.directives)
            return

        val ir = testServices.artifactsProvider.getArtifact(module, BackendKinds.IrBackend)

        val functionsUnderTest = collectInlineFunctions(ir.irModuleFragment)
        if (functionsUnderTest.isEmpty()) return

        functionsUnderTestCounter += functionsUnderTest.size

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val irBuiltIns = ir.irPluginContext.irBuiltIns

        val deserializer = NonLinkingIrInlineFunctionDeserializer(
            irBuiltIns = irBuiltIns,
            signatureComputer = PublicIdSignatureComputer(KonanManglerIr),
        )

        val library = resolveSingleFileKlib(
            KFile(info.outputFile.absolutePath),
            configuration.getLogger(treatWarningsAsErrors = true)
        )

        for (functionUnderTest in functionsUnderTest) {
            // Make a copy of the original (fully linked) function but without the body to emulate Fir2IrLazy function.
            val deserializedContainerSource = KlibDeserializedContainerSource(
                isPreReleaseInvisible = false,
                presentableString = "Emulation of lazy IR inline function ${functionUnderTest.fullyLinkedIrFunction.render()} from ${library.libraryFile.absolutePath}",
                library
            )

            functionUnderTest.partiallyLinkedIrFunction = emulateInlineFunctionRepresentedByLazyIr(
                functionUnderTest.fullyLinkedIrFunction, deserializedContainerSource
            )
            deserializer.deserializeInlineFunction(functionUnderTest.partiallyLinkedIrFunction)
        }

        checkFunctionsSerialization(configuration, irBuiltIns, functionsUnderTest)
    }

    private fun emulateInlineFunctionRepresentedByLazyIr(
        originalFunction: IrSimpleFunction,
        deserializedContainerSource: KlibDeserializedContainerSource,
    ): IrSimpleFunction {
        assertions.assertTrue(originalFunction.isInline)
        assertions.assertFalse(originalFunction.isFakeOverride)

        // Make a bodiless copy with a specific `KlibDeserializedContainerSource` and with default values
        // in parameters substituted by stubs.
        val newFunction = originalFunction.deepCopyForLazyIrEmulation(deserializedContainerSource)

        // If it is a property accessor, also make a lightweight copy of the property.
        // This is necessary to properly compute signatures for accessors.
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        originalFunction.correspondingPropertySymbol?.owner?.let { originalProperty ->
            val newProperty = originalProperty.factory.buildProperty {
                updateFrom(originalProperty)
                name = originalProperty.name
            }
            newProperty.parent = originalProperty.parent

            newFunction.correspondingPropertySymbol = newProperty.symbol
            if (originalFunction.isGetter) newProperty.getter = newFunction else newProperty.setter = newFunction
        }

        return newFunction
    }

    private fun checkFunctionsSerialization(
        configuration: CompilerConfiguration,
        irBuiltIns: IrBuiltIns,
        functionsUnderTest: Set<InlineFunctionUnderTest>
    ) {
        for (function in functionsUnderTest) {
            function.fullyLinkedSerializedFunction = SingleFunctionSerializer(
                IrSerializationSettings(
                    languageVersionSettings = configuration.languageVersionSettings,
                    reuseExistingSignaturesForSymbols = false,
                ),
                irBuiltIns
            ).serializeSingleFunction(function.fullyLinkedIrFunction)
        }

        for (function in functionsUnderTest) {
            function.partiallyLinkedSerializedFunction = SingleFunctionSerializer(
                IrSerializationSettings(
                    languageVersionSettings = configuration.languageVersionSettings,
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
                    reuseExistingSignaturesForSymbols = true
                ),
                irBuiltIns
            ).serializeSingleFunction(function.partiallyLinkedIrFunction)
        }

        for (function in functionsUnderTest) {
            testServices.assertions.assertSerializedFunctionsEqual(function)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        // It does not make sense to keep tests without a single inline function as "green", because nothing
        // effectively has been tested there. Neither makes it sense to keep them red. Thus, muted.
        assumeTrue(functionsUnderTestCounter > 0, "No inline functions found for test")
    }

    companion object {
        private fun collectInlineFunctions(irFragment: IrModuleFragment): Set<InlineFunctionUnderTest> {
            val result = hashSetOf<InlineFunctionUnderTest>()

            irFragment.acceptVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    if (declaration.isInline && !declaration.isEffectivelyPrivate()) {
                        // If this is a fake override, then it has no body and nothing specifically can be deserialized for
                        // the function itself. Instead, we shall deserialize the body of the resolved fake override,
                        // which eventually will be copied to the call site during inlining.
                        val resolvedFakeOverride = declaration.resolveFakeOverrideOrFail()
                        if (resolvedFakeOverride !is IrLazyDeclarationBase) {
                            result += InlineFunctionUnderTest(fullyLinkedIrFunction = resolvedFakeOverride)
                        }
                    }
                    super.visitSimpleFunction(declaration)
                }
            })

            return result
        }

        private fun Assertions.assertSerializedFunctionsEqual(function: InlineFunctionUnderTest) {
            val functionDescription = function.fullyLinkedIrFunction.render()

            val a = function.fullyLinkedSerializedFunction
            val b = function.partiallyLinkedSerializedFunction

            assertTrue(areEqual(a.functionProto, b.functionProto)) { "Different byte sequence for function proto of $functionDescription" }

            assertListsEqual(a, b, SerializedFunction::protoTypes, ::areEqual) { "types for $functionDescription" }
            assertListsEqual(a, b, SerializedFunction::protoIdSignatures, ::areEqual) { "ID signatures for $functionDescription" }
            assertListsEqual(a, b, SerializedFunction::protoStrings, String::equals) { "strings for $functionDescription" }
            assertListsEqual(a, b, SerializedFunction::protoBodies, ::areEqual) { "proto bodies for $functionDescription" }
            assertListsEqual(a, b, SerializedFunction::protoDebugInfos, ::areDebugInfosEqual) { "debug infos for $functionDescription" }
        }

        private fun <T : ProtoMessage> areEqual(a: T, b: T): Boolean = a.toByteArray() contentEquals b.toByteArray()
        private fun <T : XStatementOrExpression> areEqual(a: T, b: T): Boolean = a.toByteArray() contentEquals b.toByteArray()

        private fun areDebugInfosEqual(a: String, b: String): Boolean {
            // We store rendered IR declaration text in 'debug info' for signatures of local declarations.
            // If it happens, that a declaration has a type which refers to an unbound IR classifier symbol,
            // then the rendered IR text would contain `<unbound Ir*SymbolImpl>`.
            //
            // Examples of 'debug info':
            // Bound symbol: CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[a.I]
            // Unbound symbol: CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[<unbound IrClassSymbolImpl>]
            //
            // To compare 'debug infos' computed with bound and unbound symbols, it's required to split the string
            // into tokens between "<unbound Ir([a-zA-Z]+>)" and make sure that every token is present in both 'debug infos'.
            if ("<unbound Ir" in b && "<unbound Ir" !in a) {
                var searchIndex = 0
                for (segment in b.split(UNBOUND_SYMBOL_IN_DEBUG_INFO)) {
                    val index = a.indexOf(segment, searchIndex)
                    if (index < 0) return false
                    searchIndex = index + segment.length
                }
                return true
            }

            return a == b
        }

        private inline fun <T : Any> Assertions.assertListsEqual(
            left: SerializedFunction,
            right: SerializedFunction,
            propertyReference: KProperty1<SerializedFunction, List<T>>,
            crossinline areEqual: (T, T) -> Boolean,
            crossinline suffix: () -> String,
        ) {
            val leftList = propertyReference.get(left)
            val rightList = propertyReference.get(right)

            assertEquals(leftList.size, rightList.size) { "Different list size of " + suffix() }

            for (index in leftList.indices) {
                assertTrue(areEqual(leftList[index], rightList[index])) { "Different contents of item $index of " + suffix() }
            }
        }

        private val UNBOUND_SYMBOL_IN_DEBUG_INFO = Regex("<unbound Ir([a-zA-Z]+>)")
    }
}

private fun IrSimpleFunction.deepCopyForLazyIrEmulation(deserializedContainerSource: KlibDeserializedContainerSource): IrSimpleFunction {
    val symbolRemapper = DeepCopySymbolRemapper()
    acceptVoid(symbolRemapper)

    val deepCopy = DeepCopyForLazyIrEmulation(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper), deserializedContainerSource)
    val newFunction = transform(deepCopy, null) as IrSimpleFunction

    return newFunction.patchDeclarationParents(initialParent = parent)
}

private class DeepCopyForLazyIrEmulation(
    private val symbolRemapper: DeepCopySymbolRemapper,
    typeRemapper: DeepCopyTypeRemapper,
    private val deserializedContainerSource: KlibDeserializedContainerSource
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        declaration.factory.createSimpleFunction(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = declaration.origin,
            name = declaration.name,
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType.remapType(),
            modality = declaration.modality,
            symbol = symbolRemapper.getDeclaredSimpleFunction(declaration.symbol),
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExternal = declaration.isExternal,
            containerSource = deserializedContainerSource, // enforce custom deserialized container source
        ).apply {
            annotations = declaration.annotations.memoryOptimizedMap { it.transform() }
            typeParameters = declaration.typeParameters.memoryOptimizedMap { it.transform() }
            body = null // enforce no body
            overriddenSymbols = declaration.overriddenSymbols.memoryOptimizedMap { symbolRemapper.getReferencedSimpleFunction(it) }
            correspondingPropertySymbol = declaration.correspondingPropertySymbol?.let(symbolRemapper::getReferencedProperty)
            processAttributes(declaration)
            parameters = declaration.parameters.memoryOptimizedMap { it.transform() }
        }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter {
        val newValueParameter = super.visitValueParameter(declaration)

        // Replace default arguments by their placeholders as if they were generated by Fir2Ir.
        if (newValueParameter.defaultValue != null) {
            newValueParameter.defaultValue = newValueParameter.createStubDefaultValue()
        }

        return newValueParameter
    }
}

private class SingleFunctionSerializer(
    settings: IrSerializationSettings,
    irBuiltIns: IrBuiltIns,
) : KonanIrFileSerializer(settings, KonanDeclarationTable(KonanGlobalDeclarationTable(irBuiltIns))) {
    fun serializeSingleFunction(irFunction: IrSimpleFunction): SerializedFunction {
        check(protoTypeArray.protoTypes.isEmpty())
        check(protoIdSignatureArray.isEmpty())
        check(protoStringArray.isEmpty())
        check(protoBodyArray.isEmpty())
        check(protoDebugInfoArray.isEmpty())

        val functionProto = inFile(irFunction.file) { serializeDeclaration(irFunction) }

        return SerializedFunction(
            functionProto = functionProto,
            protoTypes = protoTypeArray.protoTypes,
            protoIdSignatures = protoIdSignatureArray,
            protoStrings = protoStringArray,
            protoBodies = protoBodyArray,
            protoDebugInfos = protoDebugInfoArray,
        )
    }
}

private data class SerializedFunction(
    val functionProto: ProtoDeclaration,
    val protoTypes: List<ProtoType>,
    val protoIdSignatures: List<ProtoIdSignature>,
    val protoStrings: List<String>,
    val protoBodies: List<XStatementOrExpression>,
    val protoDebugInfos: List<String>,
)
