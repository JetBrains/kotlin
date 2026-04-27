/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.metadata

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.btapi.BuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.extractSourceFiles
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import kotlin.io.path.Path

internal class MetadataKlibBuildOperationFactory(private val compilerArgs: List<String>) :
    BuildOperationFactory<KotlinMetadataKlibCompilationOperation.Builder> {
    override fun createOperation(kotlinToolchains: KotlinToolchains): KotlinMetadataKlibCompilationOperation.Builder {
        val args: K2MetadataCompilerArguments = parseCommandLineArguments(compilerArgs)
        val destination = Path(requireNotNull(args.destination))
        val compilationOperationBuilder =
            kotlinToolchains.metadata.metadataKlibCompilationOperationBuilder(extractSourceFiles(args.freeArgs), destination)
        args.destination = null // TODO: KT-85394 refactor setting up arguments to avoid this hack
        @OptIn(ExperimentalCompilerArgument::class)
        compilationOperationBuilder.compilerArguments.applyArgumentStrings(
            args.toArgumentStrings(
                allowArgFileInValues = false
            )
        )
        return compilationOperationBuilder
    }
}
