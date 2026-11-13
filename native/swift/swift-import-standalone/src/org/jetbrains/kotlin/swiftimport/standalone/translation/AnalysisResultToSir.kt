/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.translation

import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.util.addChild
import org.swift.swiftkit.core.SwiftArena
import org.swift.swiftkit.swiftextract.AnalysisResult
import org.swift.swiftkit.swiftextract.ExtractedFunc
import org.swift.swiftkit.swiftextract.ExtractedSwiftDecl

/**
 * Translates top-level functions without parameters of [this] analysis result to SIR. All other declarations are ignored.
 * Types are not translated yet: all of them are [SirUnsupportedType].
 */
internal fun AnalysisResult.toSirModule(moduleName: String, arena: SwiftArena): SirModule = buildModule {
    name = moduleName
}.apply {
    getExtractedGlobalFuncs(arena)
        .forEach { func -> addChild { func.toSirFunction() } }
}

private fun ExtractedFunc.toSirFunction(): SirFunction = buildFunction {
    name = this@toSirFunction.name
    returnType = SirUnsupportedType
    origin = KotlinSource(this@toSirFunction)
}

public open class KotlinSource(
    public val swiftDecl: ExtractedSwiftDecl,
) : SirOrigin.Foreign.SourceCode
