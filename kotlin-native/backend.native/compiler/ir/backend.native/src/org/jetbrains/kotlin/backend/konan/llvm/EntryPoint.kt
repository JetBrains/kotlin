/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal fun findMainEntryPoint(context: Context): FunctionDescriptor? {

    val config = context.config.configuration
    if (config.get(KonanConfigKeys.PRODUCE) != PROGRAM) return null

    val entryPoint = FqName(config.get(KonanConfigKeys.ENTRY) ?: defaultEntryName(config))

    val entryName = entryPoint.shortName()
    val packageName = entryPoint.parent()

    val packageScope = context.builtIns.builtInsModule.getPackage(packageName).memberScope

    val candidates = packageScope.getContributedFunctions(entryName,
        NoLookupLocation.FROM_BACKEND).filter {
            it.returnType?.isUnit() == true &&
            it.typeParameters.isEmpty() &&
            it.visibility.isPublicAPI
        }

    val main =
        candidates.singleOrNull { it.hasSingleArrayOfStringParameter } ?:
        candidates.singleOrNull { it.hasNoParameters } ?:
        context.reportCompilationError("Could not find '$entryName' in '$packageName' package.")

    if (main.isSuspend)
        context.reportCompilationError("Entry point can not be a suspend function.")

    return main
}

private fun defaultEntryName(config: CompilerConfiguration): String =
    when (config.get(KonanConfigKeys.GENERATE_TEST_RUNNER)) {
        TestRunnerKind.MAIN_THREAD -> "kotlin.native.internal.test.main"
        TestRunnerKind.WORKER -> "kotlin.native.internal.test.worker"
        TestRunnerKind.MAIN_THREAD_NO_EXIT -> "kotlin.native.internal.test.mainNoExit"
        else -> "main"
    }

private val KotlinType.filterClass: ClassDescriptor?
    get() {
        val constr = constructor.declarationDescriptor
        return constr as? ClassDescriptor
    }

private val ClassDescriptor.isString
    get() = fqNameSafe.asString() == "kotlin.String"

private val KotlinType.isString
    get() = filterClass?.isString ?: false

private val KotlinType.isArrayOfString: Boolean
    get() = (filterClass?.isArray ?: false) && 
            (arguments.singleOrNull()?.type?.isString ?: false)

private val FunctionDescriptor.hasSingleArrayOfStringParameter: Boolean
    get() = valueParameters.singleOrNull()?.type?.isArrayOfString ?: false

private val FunctionDescriptor.hasNoParameters: Boolean
    get() = valueParameters.isEmpty()
