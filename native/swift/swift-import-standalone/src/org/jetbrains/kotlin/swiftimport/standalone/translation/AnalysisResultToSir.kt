/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.translation

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftimport.standalone.session.SwiftImportSirSession
import org.swift.swiftkit.core.SwiftArena
import org.swift.swiftkit.swiftextract.AnalysisResult
import org.swift.swiftkit.swiftextract.ExtractedFunc
import org.swift.swiftkit.swiftextract.ExtractedSwiftDecl

/**
 * Translates top-level functions without parameters of [this] analysis result to SIR. All other declarations are ignored.
 * Types are not translated yet: all functions are considered to return [SirType.void].
 *
 * Every function gets the bridges that let Kotlin call it (see [SwiftImportSirSession.reverseBridges]).
 */
internal fun AnalysisResult.toSirModule(moduleName: String, arena: SwiftArena): SirModule {
    val sirModule = buildModule { name = moduleName }
    SwiftImportSirSession.create(sirModule).use { session ->
        getExtractedGlobalFuncs(arena)
            .forEach { func -> sirModule.addChild { func.toSirFunction(moduleName, session) } }
    }
    return sirModule
}

private fun ExtractedFunc.toSirFunction(moduleName: String, session: SwiftImportSirSession): SirFunction = buildFunction {
    name = this@toSirFunction.name
    returnType = SirType.void
    origin = KotlinSource(this@toSirFunction)
    bridges += session.reverseBridges(moduleName, this@toSirFunction.name)
}

/**
 * Generates the bridges for calling the Swift function `<moduleName>.<functionName>()` from Kotlin.
 * It is the same machinery Swift Export uses for calling Swift overrides: Kotlin gets an
 * `@ImportedBridge external fun`, and Swift gets an `@_cdecl` function that calls the original one.
 */
private fun SwiftImportSirSession.reverseBridges(moduleName: String, functionName: String): List<SirBridge> {
    val proxy = generateFunctionBridge(
        baseBridgeName = "${moduleName}_$functionName",
        explicitParameters = emptyList(),
        returnType = SirType.void,
        kotlinFqName = FqName("$moduleName.$functionName"),
        kotlinOptIns = emptyList(),
        selfParameter = null,
        contextParameters = emptyList(),
        extensionReceiverParameter = null,
        errorParameter = null,
        isAsync = false,
    ) ?: return emptyList()
    return withSessions {
        proxy.createReverseInvocationBridges { "$moduleName.$functionName()" }
    }
}

public open class KotlinSource(
    public val swiftDecl: ExtractedSwiftDecl,
) : SirOrigin.Foreign.SourceCode
