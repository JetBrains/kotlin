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

/*
 * Sometimes psi2ir produces IR with non-trivial variance in super types of SAM conversions (this is a language design issue).
 * Earlier this was solved with just erasing all such variances but this might lead to some other hard to debug problems,
 * so after handling the majority of corner cases correctly in psi2ir it is safe to assume that such cases don't get here and
 * even if they do, then it's better to throw an error right away than to dig out weird crashes down the pipeline or even at runtime.
 * We explicitly check this, also fixing older klibs built with previous compiler versions by applying the same trick as before.
 */
//internal val checkSamSuperTypesPhase = konanUnitPhase(
//        op = {
//            // Handling types in current module not recursively:
//            // psi2ir can produce SAM conversions with variances in type arguments of type arguments.
//            // See https://youtrack.jetbrains.com/issue/KT-49384.
//            // So don't go deeper than top-level arguments to avoid the compiler emitting false-positive errors.
//            // Lowerings can handle this.
//            // Also such variances are allowed in the language for manual implementations of interfaces.
//            irModule!!.files
//                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.THROW, recurse = false).run() }
//            // TODO: This is temporary for handling klibs produced with earlier compiler versions.
//            // Handling types in dependencies recursively, just to be extra safe: don't change something that works.
//            irModules.values
//                    .flatMap { it.files }
//                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.ERASE, recurse = true).run() }
//        },
//        name = "CheckSamSuperTypes",
//        description = "Check SAM conversions super types"
//)


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
//        disable(checkSamSuperTypesPhase)
        disableUnless(exportInternalAbiPhase, config.produce.isCache)
        disableUnless(stringConcatenationTypeNarrowingPhase, config.optimizationsEnabled)
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
    }
}
