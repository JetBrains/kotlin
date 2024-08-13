/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclarationContainer
import org.jetbrains.kotlin.sir.allClasses
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.util.objCClassName
import org.jetbrains.kotlin.swiftexport.compilerconfig.ClassNamingMapping
import org.jetbrains.kotlin.swiftexport.compilerconfig.CompilerConfig

private val runtimeClassNamingMappings: List<ClassNamingMapping>
    get() = listOf(
        ClassNamingMapping(kotlinClassFqName = "kotlin.Any", objCClassName = "KotlinBase"),
    )

private val SirClass.classNamingMapping: ClassNamingMapping?
    get() {
        val kotlinClassFqName = ((origin as? KotlinSource)?.symbol as? KaNamedClassSymbol)?.classId?.asFqNameString() ?: return null
        return ClassNamingMapping(kotlinClassFqName, objCClassName)
    }

private val SirDeclarationContainer.classNamingMappings: Sequence<ClassNamingMapping>
    get() {
        return allClasses().mapNotNull { it.classNamingMapping }
    }

internal fun buildCompilerConfig(container: SirDeclarationContainer) = CompilerConfig(
    classNamingMappings = runtimeClassNamingMappings + container.classNamingMappings,
)