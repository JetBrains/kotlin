/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.gradle.idea.IdeaKotlinFragment
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.MutableExtras


sealed interface IdeaKotlinCompilation : HasMutableExtras {
    val compilationName: String
    val fragmentName: String
    val compilerType: IdeaKotlinCompilerType
    val compilerArguments: List<String>
    val outputDirs: IdeaKotlinClasspath
    val compileTaskName: String
}

fun IdeaKotlinCompilation(
    coordinates: IdeaKotlinCompilationCoordinates,
    compilerType: IdeaKotlinCompilerType,
    compilerArguments: List<String>,
    fragments: Set<IdeaKotlinFragment>,
    outputDirs: IdeaKotlinClasspath,
    compileTaskName: String,
    extras: MutableExtras,
): IdeaKotlinCompilation = IdeaKotlinCompilationImpl(
    coordinates = coordinates,
    compilerType = compilerType,
    compilerArguments = compilerArguments,
    fragments = fragments,
    outputDirs = outputDirs,
    compileTaskName = compileTaskName,
    extras = extras
)

private data class IdeaKotlinCompilationImpl(
    override val coordinates: IdeaKotlinCompilationCoordinates,
    override val compilerType: IdeaKotlinCompilerType,
    override val compilerArguments: List<String>,
    override val fragments: Set<IdeaKotlinFragment>,
    override val outputDirs: IdeaKotlinClasspath,
    override val compileTaskName: String,
    override val extras: MutableExtras,
) : IdeaKotlinCompilation


fun IdeaKotlinCompilationCoordinates(targetName: String, compilationName: String): IdeaKotlinCompilationCoordinates =
    IdeaKotlinCompilationCoordinatesImpl(targetName, compilationName)

sealed interface IdeaKotlinCompilationCoordinates {
    val targetName: String
    val compilationName: String
}

private data class IdeaKotlinCompilationCoordinatesImpl(
    override val targetName: String,
    override val compilationName: String,
) : IdeaKotlinCompilationCoordinates
