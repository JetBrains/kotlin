/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.isNever
import org.jetbrains.kotlin.sir.util.isVoid
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal const val exportAnnotationFqName = "kotlin.native.internal.ExportedBridge"
private const val cinterop = "kotlinx.cinterop.*"
private const val convertBlockPtrToKotlinFunction = "kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction"
private const val stdintHeader = "stdint.h"
private const val foundationHeader = "Foundation/Foundation.h"

public class SirBridgeProviderImpl(private val session: SirSession, private val typeNamer: SirTypeNamer) : SirBridgeProvider {
    override fun generateTypeBridge(
        kotlinFqName: FqName?,
        swiftFqName: String,
        swiftSymbolName: String,
    ): SirTypeBindingBridge? {
        if (kotlinFqName != null && session.isFqNameSupported(kotlinFqName)) return null

        val annotationName = "kotlin.native.internal.objc.BindClassToObjCName"
        val kotlinFqName = kotlinFqName?.asString() ?: ""
        return SirTypeBindingBridge(
            name = swiftFqName,
            kotlinFileAnnotation = "$annotationName($kotlinFqName::class, \"$swiftSymbolName\")"
        )
    }

    override fun generateFunctionBridge(
        baseBridgeName: String,
        explicitParameters: List<SirParameter>,
        returnType: SirType,
        kotlinFqName: FqName,
        selfParameter: SirParameter?,
        contextParameters: List<SirParameter>,
        extensionReceiverParameter: SirParameter?,
        errorParameter: SirParameter?,
        isAsync: Boolean,
    ): BridgeFunctionProxy? = session.withSessions {
        val covariantTypes = listOfNotNull(returnType, errorParameter?.type)
        val contravariantTypes = (explicitParameters + listOfNotNull(selfParameter, extensionReceiverParameter))
            .map { it.type }

        if ((covariantTypes + contravariantTypes).any { !isSupported(it) })
            return@withSessions null

        // If any of the parameters is never - there should be no ability to call this function - therefore we can skip the bridge generation
        if (contravariantTypes.any { it.isNever })
            return@withSessions null

        BridgeFunctionDescriptor(
            baseBridgeName = baseBridgeName,
            parameters = explicitParameters.mapIndexed { index, value -> bridgeParameter(value, index) },
            returnType = bridgeReturnType(returnType),
            kotlinFqName = kotlinFqName,
            selfParameter = selfParameter?.let { bridgeParameter(it, 0) },
            contextParameters = contextParameters.mapIndexed { index, value -> bridgeParameter(value, index) },
            extensionReceiverParameter = extensionReceiverParameter?.let { bridgeParameter(it, 0) },
            errorParameter = run {
                isAsync.ifTrue {
                    Bridge.AsOptionalWrapper(
                        Bridge.AsObject(
                            swiftType = KotlinRuntimeModule.kotlinBase.nominalType(),
                            kotlinType = KotlinType.KotlinObject,
                            cType = CType.Object
                        )
                    )
                } ?: errorParameter?.let {
                    Bridge.AsOutError
                }
            }?.let {
                BridgedParameter.InOut(
                    name = (errorParameter?.name ?: "error").let(::createBridgeParameterName),
                    bridge = it
                )
            },
            isAsync = isAsync,
            typeNamer = typeNamer,
        )
    }
}

context(ka: KaSession, sir: SirSession)
internal fun isSupported(type: SirType): Boolean = when (type) {
    is SirNominalType -> {
        val declarationSupported = when (val declaration = type.typeDeclaration) {
            is SirTypealias -> isSupported(declaration.type)
            else -> type.typeDeclaration.kaSymbolOrNull<KaNamedClassSymbol>()?.sirAvailability()?.let { it is SirAvailability.Available } != false
        }
        declarationSupported && type.typeArguments.all { isSupported(it) }
    }
    is SirFunctionalType -> isSupported(type.returnType) && type.parameterTypes.all { isSupported(it) }
    is SirExistentialType -> type.protocols.all {
        it == KotlinRuntimeSupportModule.kotlinBridgeable ||
                it.kaSymbolOrNull<KaClassSymbol>()?.sirAvailability() is SirAvailability.Available
    }
    else -> false
}

public interface BridgeFunctionBuilder {
    public val baseBridgeName: String
    public val kotlinFqName: FqName
    public val typeNamer: SirTypeNamer

    public val parameters: List<Any>
    public val returnType: Any
    public val selfParameter: Any?
    public val contextParameters: List<Any>
    public val extensionReceiverParameter: Any?
    public val errorParameter: Any?
    public val isAsync: Boolean

    public fun buildCall(args: String, selfCastType: String? = null): String
    public val argNames: List<String>
    public val name: String
}

public interface BridgeFunctionProxy {
    context(sir: SirSession)
    public fun createSirBridges(kotlinCall: BridgeFunctionBuilder.() -> String): List<SirBridge>
    public fun createSwiftInvocation(resultTransformer: ((String) -> String)?): List<String>
    public fun argumentsForInvocation(): List<String>
}

private class BridgeFunctionDescriptor(
    override val baseBridgeName: String,
    override val parameters: List<BridgedParameter>,
    override val returnType: KotlinToSwiftBridge,
    override val kotlinFqName: FqName,
    override val selfParameter: BridgedParameter?,
    override val contextParameters: List<BridgedParameter>,
    override val extensionReceiverParameter: BridgedParameter?,
    override val errorParameter: BridgedParameter.InOut?,
    override val isAsync: Boolean,
    override val typeNamer: SirTypeNamer,
) : BridgeFunctionBuilder, BridgeFunctionProxy {
    val kotlinBridgeName = bridgeDeclarationName(baseBridgeName, parameters, typeNamer)
    val cBridgeName = kotlinBridgeName

    val allParameters
        get() = listOfNotNull(selfParameter) + parameters + listOfNotNull(errorParameter.takeIf { !isAsync }) +
                (asyncParameters?.toList() ?: emptyList())

    val asyncParameters: Triple<BridgedParameter.In, BridgedParameter.In, BridgedParameter.In>? by lazy {
        isAsync.ifTrue {
            Triple(
                BridgedParameter.In(
                    name = "continuation",
                    bridge = Bridge.AsContravariantBlock(parameters = listOf(returnType), returnType = Bridge.AsOutVoid)
                ),
                BridgedParameter.In(
                    name = "exception",
                    bridge = Bridge.AsContravariantBlock(parameters = listOfNotNull(errorParameter?.bridge), returnType = Bridge.AsOutVoid)
                ),
                BridgedParameter.In(
                    name = "cancellation",
                    bridge = Bridge.AsObject(
                        swiftType = KotlinCoroutineSupportModule.swiftJob.nominalType(),
                        kotlinType = KotlinType.KotlinObject,
                        cType = CType.Object,
                    )
                ),
            )
        }
    }

    override val name
        get() = kotlinFqName.pathSegments().joinToString(separator = ".") { it.asString().kotlinIdentifier }

    override val argNames
        get() = buildList {
            var useNamed = false
            parameters.forEachIndexed { _, bridgeParameter ->
                val argName = buildString {
                    if (bridgeParameter.bridge is Bridge.AsNSArrayForVariadic) {
                        append("*")
                        useNamed = true
                    } else if (useNamed && bridgeParameter.isExplicit) {
                        append("${bridgeParameter.name} = ")
                    }
                    append("__${bridgeParameter.name}".kotlinIdentifier)
                }
                add(argName)
            }
        }

    override fun buildCall(args: String, selfCastType: String?): String {
        var result = if (selfParameter == null) {
            if (extensionReceiverParameter == null) {
                "$name$args"
            } else {
                "__${extensionReceiverParameter.name}.$safeImportName$args"
            }
        } else {
            val memberName = kotlinFqName.shortName().asString().kotlinIdentifier
            val selfRef = if (selfCastType != null) {
                "(__${selfParameter.name} as $selfCastType)"
            } else {
                "__${selfParameter.name}"
            }
            if (extensionReceiverParameter == null) {
                "$selfRef.$memberName$args"
            } else {
                "$selfRef.run { __${extensionReceiverParameter.name}.$memberName$args }"
            }
        }
        if (contextParameters.isNotEmpty()) {
            result = "context(${contextParameters.joinToString { "__${it.name}".kotlinIdentifier }}) { $result }"
        }
        return result
    }

    context(sir: SirSession)
    override fun createSirBridges(kotlinCall: BridgeFunctionBuilder.() -> String): List<SirBridge> {
        return buildList {
            add(
                SirFunctionBridge(
                    name = baseBridgeName,
                    KotlinFunctionBridge(
                        createKotlinBridge(typeNamer, kotlinCall),
                        listOf(exportAnnotationFqName, cinterop, convertBlockPtrToKotlinFunction) + additionalImports()
                    ),
                    CFunctionBridge(listOf(cDeclaration()), listOf(foundationHeader, stdintHeader))
                )
            )
            val allBridges = parameters.mapTo(mutableListOf<AnyBridge>()) { it.bridge }
            selfParameter?.let { allBridges.add(it.bridge) }
            extensionReceiverParameter?.let { allBridges.add(it.bridge) }
            allBridges.add(returnType)
            allBridges.forEach {
                if (it.typeList.size <= 1) return@forEach
                for (i in 0..<it.typeList.size) {
                    add(it.nativePointerToMultipleObjCBridge(i))
                }
            }

            // todo: KT-82908 Swift Export: bridges for FT should be recursive
            allBridges
                .flatMap { it.helperBridges(typeNamer) }
                .forEach { add(it) }
        }.distinct()
    }

    override fun createSwiftInvocation(resultTransformer: ((String) -> String)?): List<String> = buildList {
        val descriptor = this@BridgeFunctionDescriptor
        val contextParameters = descriptor.contextParameters
        val errorParameter = descriptor.errorParameter

        if (contextParameters.isNotEmpty()) {
            add("let (${contextParameters.joinToString { it.name.swiftIdentifier }}) = context")
        }
        if (isAsync) {
            add(descriptor.swiftAsyncCall(typeNamer))
        } else if (errorParameter != null) {
            add("var ${errorParameter.name}: UnsafeMutableRawPointer? = nil")
            add("let _result = ".takeIf { resultTransformer != null }.orEmpty() + descriptor.swiftInvocationLineForCBridge(typeNamer))
            val error = errorParameter.bridge.inSwiftSources.kotlinToSwift(typeNamer, errorParameter.name)
            add("guard ${errorParameter.name} == nil else { throw KotlinError(wrapped: $error) }")
            resultTransformer?.let { add(it(descriptor.returnType.inSwiftSources.kotlinToSwift(typeNamer, "_result"))) }
        } else {
            val swiftCallAndTransformationLines = descriptor.swiftLinesForCBridgeCallAndTransformation(typeNamer)
            addAll(swiftCallAndTransformationLines.dropLast(1))
            add((resultTransformer ?: { it })(swiftCallAndTransformationLines.last()))
        }
    }

    override fun argumentsForInvocation(): List<String> = allParameters.filter { it.isRenderable }.map {
        it.name.takeIf { it == "self" } ?: it.name.swiftIdentifier
    }
}

// TODO: we need to mangle C name in more elegant way. KT-64970
// problems with this approach are:
// 1. there can be limit for declaration names in Clang compiler
// 1. this name will be UGLY in the debug session
private fun bridgeDeclarationName(bridgeName: String, parameterBridges: List<BridgedParameter>, typeNamer: SirTypeNamer): String {
    val nameSuffixForOverloadSimulation = parameterBridges.joinToString(separator = "_") {
        typeNamer.swiftFqName(it.bridge.swiftType)
            .replace(".", "_")
            .replace(",", "_")
            .replace("<", "_")
            .replace(">", "_") +
                if (it.bridge is Bridge.AsNSArrayForVariadic) "_Vararg_" else ""
    }
    val suffixString = if (parameterBridges.isNotEmpty()) "__TypesOfArguments__${nameSuffixForOverloadSimulation}__" else ""
    val result = "${bridgeName}${suffixString}".cIdentifier
    return result
}

private fun BridgeFunctionDescriptor.createKotlinBridge(
    typeNamer: SirTypeNamer,
    buildCallSite: BridgeFunctionDescriptor.() -> String,
) = buildList {
    add("@${exportAnnotationFqName.substringAfterLast('.')}(\"${cBridgeName}\")")
    val kotlinReturnType = when {
        returnType.typeList.isEmpty() -> KotlinType.Unit
        returnType.typeList.size == 1 -> returnType.typeList.single().kotlinType.takeIf { !isAsync } ?: KotlinType.Unit
        // As we can't return multiple types, we use native pointer instead
        else -> KotlinType.KotlinObject
    }
    add(
        "public fun $kotlinBridgeName(${
            allParameters.filter { it.isRenderable && it.bridge.typeList.isNotEmpty() }.joinToString {
                val bridge = it.bridge
                val identifier = it.name.kotlinIdentifier
                if (bridge.typeList.size > 1) {
                    var index = 0
                    bridge.typeList.joinToString { (kotlinType, _) ->
                        index++
                        "${identifier}_$index: ${kotlinType.repr}"
                    }
                } else {
                    val typeRepresentation = bridge.typeList.single().kotlinType.repr
                    "$identifier: $typeRepresentation"
                }
            }
        }): ${kotlinReturnType.repr} {"
    )
    val indent = "    "

    allParameters.forEach {
        val parameterName = "__${it.name}".kotlinIdentifier
        add("${indent}val $parameterName = ${it.bridge.inKotlinSources.swiftToKotlin(typeNamer, it.name.kotlinIdentifier)}")
    }
    val callSite = buildCallSite()
    val resultName = "_result"

    if (isAsync) {
        val (continuation, exception, cancellation) = asyncParameters ?: error("Async function must have a continuation & cancellation")
        val errorParameter = errorParameter ?: error("Async function must have an error parameter")
        add(
            """
            CoroutineScope(__${cancellation.name.kotlinIdentifier} + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    val $resultName = $callSite
                    __${continuation.name}(${resultName})
                } catch (error: CancellationException) {
                    __${cancellation.name.kotlinIdentifier}.cancel()
                    __${exception.name}(null)
                    throw error
                } catch (error: Throwable) {
                    __${exception.name}(error)
                }
            }.alsoCancel(__${cancellation.name.kotlinIdentifier})
            """.trimIndent().prependIndent(indent)
        )
    } else if (returnType.swiftType.isVoid && errorParameter == null) {
        add("${indent}$callSite")
    } else {
        if (errorParameter != null) {
            // TODO: is it correct to use the first type only here?
            val defaultValue = returnType.typeList.first().kotlinType.defaultValue
            add(
                """
            try {
                val $resultName = $callSite
                return ${returnType.inKotlinSources.kotlinToSwift(typeNamer, resultName)}
            } catch (error: Throwable) {
                __${errorParameter.name}.value = StableRef.create(error).asCPointer()
                return $defaultValue
            }
            """.trimIndent().prependIndent(indent)
            )
        } else {
            add("${indent}val $resultName = $callSite")
            add("${indent}return ${returnType.inKotlinSources.kotlinToSwift(typeNamer, resultName)}")
        }
    }
    add("}")
}

private fun BridgeFunctionDescriptor.swiftInvocationLineForCBridge(typeNamer: SirTypeNamer): String {
    val parameters = allParameters.filter { it.isRenderable }.joinToString {
        // We fix ugly `self` escaping here. This is the only place we'd otherwise need full support for swift's contextual keywords
        it.bridge.inSwiftSources.swiftToKotlin(typeNamer, it.name.takeIf { it == "self" } ?: it.name.swiftIdentifier)
    }
    return "$cBridgeName($parameters)"
}

private fun BridgeFunctionDescriptor.swiftLinesForCBridgeCallAndTransformation(typeNamer: SirTypeNamer): List<String> {
    val swiftInvocation = swiftInvocationLineForCBridge(typeNamer)
    if (returnType.typeList.size <= 1) {
        return listOf(returnType.inSwiftSources.kotlinToSwift(typeNamer, swiftInvocation))
    }
    return buildList {
        add("let _result = $swiftInvocation")
        add(returnType.inSwiftSources.kotlinToSwift(typeNamer, "_result"))
    }
}

private fun BridgeFunctionDescriptor.swiftAsyncCall(typeNamer: SirTypeNamer): String {
    val (continuation, exception, cancellation) = asyncParameters ?: error("Async function must have a continuation & cancellation")
    val errorParameter = errorParameter ?: error("Async function must have an error parameter")
    val indent = "                        "

    return """
        try${"!".takeIf { !isAsync } ?: ""} await {
            try Task.checkCancellation()
            var ${cancellation.name.swiftIdentifier}: ${cancellation.bridge.swiftType.swiftName}! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let ${continuation.name.swiftIdentifier}: ${continuation.bridge.swiftType.swiftName} = { nativeContinuation.resume(returning: $0) }
                        let ${exception.name.swiftIdentifier}: ${exception.bridge.swiftType.swiftName} = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        ${cancellation.name.swiftIdentifier} = ${cancellation.bridge.swiftType.swiftName}(currentTask!)
                        
                        let _: () = ${swiftInvocationLineForCBridge(typeNamer).prependIndentToTrailingLines(indent)}
                    }            
                }
            } onCancel: {
                ${cancellation.name.swiftIdentifier}?.cancelExternally()
            }
        }()
    """.trimIndent()
}

private fun BridgeFunctionDescriptor.cDeclaration() = buildString {
    val returnTypeBridge = returnType.takeIf { !isAsync } ?: Bridge.AsVoid
    val returnType = when {
        returnTypeBridge.typeList.isEmpty() -> CType.Void
        returnTypeBridge.typeList.size == 1 -> returnTypeBridge.typeList.single().cType
        // As we can't return multiple types, we use void* here
        else -> CType.Object
    }

    append(
        returnType.render(buildString {
            append(cBridgeName)
            append("(")
            allParameters.filter { it.isRenderable && it.bridge.typeList.isNotEmpty() }.joinTo(this) {
                val bridge = it.bridge
                if (bridge.typeList.size > 1) {
                    var index = 0
                    bridge.typeList.joinToString { (_, cType) ->
                        index++
                        cType.render("${it.name.cIdentifier}_$index")
                    }
                } else {
                    bridge.typeList.single().cType.render(it.name.cIdentifier)
                }
            }
            append(')')
        })
    )
    if (returnTypeBridge.swiftType.isNever) append(" __attribute((noreturn))")
    append(";")
}

private fun BridgeFunctionDescriptor.additionalImports(): List<String> = listOfNotNull(
    (extensionReceiverParameter != null && selfParameter == null && !kotlinFqName.parent().isRoot).ifTrue {
        "$name as $safeImportName"
    },
    isAsync.ifTrue {
        "kotlinx.coroutines.*"
    },
)

private val BridgeFunctionDescriptor.safeImportName: String
    get() = kotlinFqName.pathSegments().joinToString(separator = "_") { it.asString().replace("_", "__") }

private fun String.prependIndentToTrailingLines(indent: String): String = this.lines().let { lines ->
    lines.singleOrNull() ?: buildString {
        append(lines.first())
        for (line in lines.drop(1)) {
            append('\n')
            append(indent)
            append(line)
        }
    }
}
