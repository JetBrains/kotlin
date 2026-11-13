/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.translation

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.swiftimport.standalone.session.SwiftImportSirSession
import org.swift.swiftkit.core.SwiftArena
import org.swift.swiftkit.swiftextract.AnalysisResult
import org.swift.swiftkit.swiftextract.ExtractedFunc
import org.swift.swiftkit.swiftextract.ExtractedNominalType
import org.swift.swiftkit.swiftextract.ExtractedSwiftDecl

/**
 * Translates top-level functions and classes of [this] analysis result to SIR. All other declarations are ignored.
 * Only functions, methods and initializers without parameters are supported. Types are not translated yet:
 * functions and methods must return `Void`, and initializers must not be failable.
 *
 * The imported module is a [SirPlatformModule]: Swift classes are exposed to Kotlin as Objective-C objects,
 * exactly like the classes of platform modules are exposed in Swift Export. As a result, Swift Export's bridges pass them as `NSObject`.
 *
 * Every callable gets the bridges that let Kotlin call it (see [SwiftImportSirSession.reverseBridges]).
 */
internal fun AnalysisResult.toSirModule(moduleName: String, arena: SwiftArena): SirModule {
    val sirModule = SirPlatformModule(moduleName)
    SwiftImportSirSession.create(sirModule).use { session ->
        getExtractedGlobalFuncs(arena)
            .filter { it.isSupportedFunction(arena) }
            .forEach { func -> sirModule.addChild { func.toSirFunction(moduleName, session) } }
        // Swift dictionaries have no stable order, so classes are ordered by name to keep the output deterministic.
        getExtractedTypes(arena).toJavaMap().toSortedMap().values
            .filter { it.isSupportedClass(arena) }
            .forEach { type -> type.toSirClass(sirModule, session, arena) }
    }
    return sirModule
}

private fun ExtractedFunc.toSirFunction(moduleName: String, session: SwiftImportSirSession): SirFunction = buildFunction {
    name = this@toSirFunction.name
    returnType = SirType.void
    isInstance = false
    origin = KotlinSource(this@toSirFunction)
    bridges += session.reverseBridges(
        baseBridgeName = "${moduleName}_$name",
        kotlinFqName = FqName("$moduleName.$name"),
    ) { _, _ -> "$moduleName.$name()" }
}

private fun ExtractedNominalType.toSirClass(sirModule: SirModule, session: SwiftImportSirSession, arena: SwiftArena) {
    val sirClass = sirModule.addChild { SirClassFromSwift(name = effectiveSwiftTypeName, origin = KotlinSource(this)) }
    getInitializers(arena)
        .filter { it.isSupportedInitializer(arena) }
        .forEach { init -> sirClass.addMember { init.toSirInit(sirClass, session) } }
    getMethods(arena)
        .filter { it.isSupportedFunction(arena) }
        .forEach { method -> sirClass.addMember { method.toSirMethod(sirClass, session) } }
}

private fun ExtractedFunc.toSirInit(sirClass: SirClassFromSwift, session: SwiftImportSirSession): SirInit = buildInit {
    isFailable = false
    origin = KotlinSource(this@toSirInit)
    bridges += session.reverseBridges(
        baseBridgeName = "${sirClass.bridgeNamePrefix}_init",
        kotlinFqName = sirClass.memberFqName("init"),
        returnType = SirNominalType(sirClass),
    ) { _, _ -> "${sirClass.swiftFqName}()" }
}

private fun ExtractedFunc.toSirMethod(sirClass: SirClassFromSwift, session: SwiftImportSirSession): SirFunction = buildFunction {
    name = this@toSirMethod.name
    returnType = SirType.void
    origin = KotlinSource(this@toSirMethod)
    bridges += session.reverseBridges(
        baseBridgeName = "${sirClass.bridgeNamePrefix}_$name",
        kotlinFqName = sirClass.memberFqName(name),
        selfType = SirNominalType(sirClass),
    ) { selfExpression, _ -> "$selfExpression.$name()" }
}

/**
 * Generates the bridges for calling a Swift callable without parameters from Kotlin.
 * It is the same machinery Swift Export uses for calling Swift overrides: Kotlin gets an
 * `@ImportedBridge external fun`, and Swift gets an `@_cdecl` function that calls the original one with [swiftCall].
 * Unlike overrides, imported Swift classes cannot be bound to the vtable of a Kotlin class,
 * so the bridges are not bound to any method.
 *
 * @param selfType the type of the Swift object a method is called on, `null` for other callables.
 * @param swiftCall builds the call of the callable given the Swift expression of the object it is called on.
 */
private fun SwiftImportSirSession.reverseBridges(
    baseBridgeName: String,
    kotlinFqName: FqName,
    returnType: SirType = SirType.void,
    selfType: SirType? = null,
    swiftCall: (selfExpression: String, parameterExpressions: List<String>) -> String,
): List<SirBridge> {
    val proxy = generateFunctionBridge(
        baseBridgeName = baseBridgeName,
        explicitParameters = emptyList(),
        returnType = returnType,
        kotlinFqName = kotlinFqName,
        kotlinOptIns = emptyList(),
        selfParameter = selfType?.let { SirParameter(parameterName = "self", type = it) },
        contextParameters = emptyList(),
        extensionReceiverParameter = null,
        errorParameter = null,
        isAsync = false,
    ) ?: return emptyList()
    return withSessions {
        proxy.createReverseInvocationBridges(swiftCall)
    }
}

private val SirClassFromSwift.bridgeNamePrefix: String
    get() = swiftFqName.replace('.', '_')

private fun SirClassFromSwift.memberFqName(memberName: String): FqName = FqName("$swiftFqName.$memberName")

private fun ExtractedFunc.isSupportedFunction(arena: SwiftArena): Boolean =
    !isStatic && !isClass && hasNoParametersAndEffects(arena) && getFunctionSignature(arena).getResult(arena).getType(arena).isVoid

/**
 * A failable initializer returns an optional, which is not a reference type.
 */
private fun ExtractedFunc.isSupportedInitializer(arena: SwiftArena): Boolean =
    hasNoParametersAndEffects(arena) && getFunctionSignature(arena).getResult(arena).getType(arena).isReferenceType

private fun ExtractedFunc.hasNoParametersAndEffects(arena: SwiftArena): Boolean =
    !isAsync && !isThrowing && getFunctionSignature(arena).getParameters(arena).isEmpty()

/**
 * Nested and generic classes are not supported yet.
 */
private fun ExtractedNominalType.isSupportedClass(arena: SwiftArena): Boolean =
    kindNameForJExtract() == "class" && getParent(arena).isEmpty && getGenericParameterNames().isEmpty()

public open class KotlinSource(
    public val swiftDecl: ExtractedSwiftDecl,
) : SirOrigin.Foreign.SourceCode
