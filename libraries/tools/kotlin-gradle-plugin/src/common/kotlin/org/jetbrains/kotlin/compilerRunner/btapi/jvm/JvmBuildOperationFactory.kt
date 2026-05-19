/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.jvm

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.btapi.BuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.extractSourceFiles
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.utils.destinationAsFile

internal class JvmBuildOperationFactory(val compilerArgs: List<String>, val kotlinScriptExtensions: List<String>) :
    BuildOperationFactory<JvmCompilationOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): JvmCompilationOperation.Builder {
        val args: K2JVMCompilerArguments = parseCommandLineArguments(compilerArgs)
        val compilationOperationBuilder = kotlinToolchains.jvm.jvmCompilationOperationBuilder(
            extractSourceFiles(args.freeArgs),
            args.destinationAsFile.toPath()
        ).also { compilationOperationBuilder ->
            args.destination = null // TODO: KT-85394 refactor setting up arguments to avoid this hack
            compilationOperationBuilder[KOTLINSCRIPT_EXTENSIONS] = kotlinScriptExtensions.toTypedArray()
        }
        compilationOperationBuilder.compilerArguments.applyArgumentStrings(args.toArgumentStrings(allowArgFileInValues = false))
        return compilationOperationBuilder
    }
}
