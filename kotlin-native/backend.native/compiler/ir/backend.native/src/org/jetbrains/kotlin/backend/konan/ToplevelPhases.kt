/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.backend.konan.lower.SamSuperTypesChecker
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName

internal fun moduleValidationCallback(state: ActionState, module: IrModuleFragment, context: Context) {
    if (!context.config.needVerifyIr) return

    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        module.accept(IrValidator(context, validatorConfig), null)
        module.checkDeclarationParents()
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal fun fileValidationCallback(state: ActionState, irFile: IrFile, context: Context) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        irFile.accept(IrValidator(context, validatorConfig), null)
        irFile.checkDeclarationParents()
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal fun konanUnitPhase(
        name: String,
        description: String,
        prerequisite: Set<AbstractNamedCompilerPhase<Context, *, *>> = emptySet(),
        op: Context.() -> Unit
) = namedOpUnitPhase(name, description, prerequisite, op)

internal val buildAdditionalCacheInfoPhase = konanUnitPhase(
        op = {
            val module = irModules[config.libraryToCache!!.klib.libraryName]
                    ?: error("No module for the library being cached: ${config.libraryToCache!!.klib.libraryName}")
            val moduleDeserializer = irLinker.moduleDeserializers[module.descriptor]
            if (moduleDeserializer == null) {
                require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
            } else {
                CacheInfoBuilder(this.generationState, moduleDeserializer, module).build()
            }
        },
        name = "BuildAdditionalCacheInfo",
        description = "Build additional cache info (inline functions bodies and fields of classes)",
// prerequisites generally do not work in dynamic driver.
//        prerequisite = setOf(psiToIrPhase)
)

/*
 * Sometimes psi2ir produces IR with non-trivial variance in super types of SAM conversions (this is a language design issue).
 * Earlier this was solved with just erasing all such variances but this might lead to some other hard to debug problems,
 * so after handling the majority of corner cases correctly in psi2ir it is safe to assume that such cases don't get here and
 * even if they do, then it's better to throw an error right away than to dig out weird crashes down the pipeline or even at runtime.
 * We explicitly check this, also fixing older klibs built with previous compiler versions by applying the same trick as before.
 */
internal val checkSamSuperTypesPhase = konanUnitPhase(
        op = {
            // Handling types in current module not recursively:
            // psi2ir can produce SAM conversions with variances in type arguments of type arguments.
            // See https://youtrack.jetbrains.com/issue/KT-49384.
            // So don't go deeper than top-level arguments to avoid the compiler emitting false-positive errors.
            // Lowerings can handle this.
            // Also such variances are allowed in the language for manual implementations of interfaces.
            irModule!!.files
                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.THROW, recurse = false).run() }
            // TODO: This is temporary for handling klibs produced with earlier compiler versions.
            // Handling types in dependencies recursively, just to be extra safe: don't change something that works.
            irModules.values
                    .flatMap { it.files }
                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.ERASE, recurse = true).run() }
        },
        name = "CheckSamSuperTypes",
        description = "Check SAM conversions super types"
)

internal val saveAdditionalCacheInfoPhase = konanUnitPhase(
        op = { CacheStorage(generationState).saveAdditionalCacheInfo() },
        name = "SaveAdditionalCacheInfo",
        description = "Save additional cache info (inline functions bodies and fields of classes)"
)

internal val objectFilesPhase = konanUnitPhase(
        op = { this.generationState.compilerOutput = BitcodeCompiler(this.generationState).makeObjectFiles(this.generationState.bitcodeFileName) },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val linkerPhase = konanUnitPhase(
        op = { Linker(this.generationState).link(this.generationState.compilerOutput) },
        name = "Linker",
        description = "Linker"
)

internal val finalizeCachePhase = konanUnitPhase(
        op = { CacheStorage(this.generationState).renameOutput() },
        name = "FinalizeCache",
        description = "Finalize cache (rename temp to the final dist)"
)

internal val allLoweringsPhase = SameTypeNamedCompilerPhase(
        name = "IrLowering",
        description = "IR Lowering",
        // TODO: The lowerings before inlinePhase should be aligned with [NativeInlineFunctionResolver.kt]
        lower = performByIrFile(
                name = "IrLowerByFile",
                description = "IR Lowering by file",
                lower = listOf(
                        createFileLowerStatePhase,
                        removeExpectDeclarationsPhase,
                        stripTypeAliasDeclarationsPhase,
                        lowerBeforeInlinePhase,
                        arrayConstructorPhase,
                        lateinitPhase,
                        sharedVariablesPhase,
                        inventNamesForLocalClasses,
                        extractLocalClassesFromInlineBodies,
                        wrapInlineDeclarationsWithReifiedTypeParametersLowering,
                        inlinePhase,
                        provisionalFunctionExpressionPhase,
                        postInlinePhase,
                        contractsDslRemovePhase,
                        annotationImplementationPhase,
                        rangeContainsLoweringPhase,
                        forLoopsPhase,
                        flattenStringConcatenationPhase,
                        foldConstantLoweringPhase,
                        computeStringTrimPhase,
                        stringConcatenationPhase,
                        stringConcatenationTypeNarrowingPhase,
                        enumConstructorsPhase,
                        initializersPhase,
                        localFunctionsPhase,
                        tailrecPhase,
                        defaultParameterExtentPhase,
                        innerClassPhase,
                        dataClassesPhase,
                        ifNullExpressionsFusionPhase,
                        testProcessorPhase,
                        delegationPhase,
                        functionReferencePhase,
                        singleAbstractMethodPhase,
                        enumWhenPhase,
                        builtinOperatorPhase,
                        finallyBlocksPhase,
                        enumClassPhase,
                        enumUsagePhase,
                        interopPhase,
                        varargPhase,
                        kotlinNothingValueExceptionPhase,
                        coroutinesPhase,
                        typeOperatorPhase,
                        expressionBodyTransformPhase,
                        objectClassesPhase,
                        constantInliningPhase,
                        staticInitializersPhase,
                        bridgesPhase,
                        exportInternalAbiPhase,
                        useInternalAbiPhase,
                        autoboxPhase,
                )
        ),
        actions = setOf(defaultDumper, ::moduleValidationCallback)
)

internal val dependenciesLowerPhase = SameTypeNamedCompilerPhase(
        name = "LowerLibIR",
        description = "Lower library's IR",
        prerequisite = emptySet(),
        lower = object : CompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                val files = mutableListOf<IrFile>()
                files += input.files
                input.files.clear()

                // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
                context.librariesWithDependencies
                        .reversed()
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                            if (libModule == null || !context.generationState.llvmModuleSpecification.containsModule(libModule))
                                return@forEach

                            input.files += libModule.files
                            allLoweringsPhase.invoke(phaseConfig, phaserState, context, input)

                            input.files.clear()
                        }

                // Save all files for codegen in reverse topological order.
                // This guarantees that libraries initializers are emitted in correct order.
                context.librariesWithDependencies
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                            if (libModule == null || !context.generationState.llvmModuleSpecification.containsModule(libModule))
                                return@forEach

                            input.files += libModule.files
                        }

                input.files += files

                return input
            }
        })

internal val entryPointPhase = makeCustomPhase<Context, IrModuleFragment>(
        name = "addEntryPoint",
        description = "Add entry point for program",
        prerequisite = emptySet(),
        op = { context, _ ->
            require(context.config.produce == CompilerOutputKind.PROGRAM)

            val entryPoint = context.ir.symbols.entryPoint!!.owner
            val file = if (context.generationState.llvmModuleSpecification.containsDeclaration(entryPoint)) {
                entryPoint.file
            } else {
                // `main` function is compiled to other LLVM module.
                // For example, test running support uses `main` defined in stdlib.
                context.irModule!!.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.internal.abi"))
            }

            file.addChild(makeEntryPoint(context.generationState))
        }
)

internal val bitcodePhase = SameTypeNamedCompilerPhase(
        name = "Bitcode",
        description = "LLVM Bitcode generation",
        lower = returnsInsertionPhase then
                buildDFGPhase then
                devirtualizationAnalysisPhase then
                dcePhase then
                removeRedundantCallsToStaticInitializersPhase then
                devirtualizationPhase then
                propertyAccessorInlinePhase then // Have to run after link dependencies phase, because fields
                                                 // from dependencies can be changed during lowerings.
                inlineClassPropertyAccessorsPhase then
                redundantCoercionsCleaningPhase then
                unboxInlinePhase then
                createLLVMDeclarationsPhase then
                ghaPhase then
                RTTIPhase then
                escapeAnalysisPhase then
                codegenPhase then
                cStubsPhase
)

internal val bitcodePostprocessingPhase = SameTypeNamedCompilerPhase(
        name = "BitcodePostprocessing",
        description = "Optimize and rewrite bitcode",
        lower = checkExternalCallsPhase then
                bitcodeOptimizationPhase then
                coveragePhase then
                removeRedundantSafepointsPhase then
                optimizeTLSDataLoadsPhase then
                rewriteExternalCallsCheckerGlobals
)

internal fun PhaseConfigurationService.disableIf(phase: AnyNamedPhase, condition: Boolean) {
    if (condition) disable(phase)
}

internal fun PhaseConfigurationService.disableUnless(phase: AnyNamedPhase, condition: Boolean) {
    if (!condition) disable(phase)
}

internal fun PhaseConfigurationService.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        // The original comment around [checkSamSuperTypesPhase] still holds, but in order to be on par with JVM_IR
        // (which doesn't report error for these corner cases), we turn off the checker for now (the problem with variances
        // is workarounded in [FunctionReferenceLowering] by taking erasure of SAM conversion type).
        // Also see https://youtrack.jetbrains.com/issue/KT-50399 for more details.
        disable(checkSamSuperTypesPhase)

        disableUnless(buildAdditionalCacheInfoPhase, config.produce.isCache)
        disableUnless(saveAdditionalCacheInfoPhase, config.produce.isCache)
        disableUnless(finalizeCachePhase, config.produce.isCache)
        disableUnless(exportInternalAbiPhase, config.produce.isCache)
        disableUnless(functionsWithoutBoundCheck, config.involvesCodegen)
        disableUnless(checkExternalCallsPhase, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(rewriteExternalCallsCheckerGlobals, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(stringConcatenationTypeNarrowingPhase, config.optimizationsEnabled)
        disableUnless(optimizeTLSDataLoadsPhase, config.optimizationsEnabled)
        if (!config.involvesLinkStage) {
            disable(bitcodePostprocessingPhase)
            disable(linkBitcodeDependenciesPhase)
            disable(objectFilesPhase)
            disable(linkerPhase)
        }
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
        if (!config.optimizationsEnabled) {
            disable(buildDFGPhase)
            disable(devirtualizationAnalysisPhase)
            disable(devirtualizationPhase)
            disable(escapeAnalysisPhase)
            // Inline accessors only in optimized builds due to separate compilation and possibility to get broken
            // debug information.
            disable(propertyAccessorInlinePhase)
            disable(unboxInlinePhase)
            disable(inlineClassPropertyAccessorsPhase)
            disable(dcePhase)
            disable(removeRedundantCallsToStaticInitializersPhase)
            disable(ghaPhase)
        }
        disableUnless(verifyBitcodePhase, config.needCompilerVerification || getBoolean(KonanConfigKeys.VERIFY_BITCODE))


        disableUnless(removeRedundantSafepointsPhase, config.memoryModel == MemoryModel.EXPERIMENTAL)
    }
}
