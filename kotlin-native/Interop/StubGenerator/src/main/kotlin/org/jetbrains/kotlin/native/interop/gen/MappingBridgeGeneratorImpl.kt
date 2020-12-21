/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.RecordType
import org.jetbrains.kotlin.native.interop.indexer.Type
import org.jetbrains.kotlin.native.interop.indexer.VoidType
import org.jetbrains.kotlin.native.interop.indexer.unwrapTypedefs

/**
 * The [MappingBridgeGenerator] implementation which uses [SimpleBridgeGenerator] as the backend and
 * maps the type using [mirror].
 */
class MappingBridgeGeneratorImpl(
        val declarationMapper: DeclarationMapper,
        val simpleBridgeGenerator: SimpleBridgeGenerator
) : MappingBridgeGenerator {

    override fun kotlinToNative(
            builder: KotlinCodeBuilder,
            nativeBacked: NativeBacked,
            returnType: Type,
            kotlinValues: List<TypedKotlinValue>,
            independent: Boolean,
            block: NativeCodeBuilder.(nativeValues: List<NativeExpression>) -> NativeExpression
    ): KotlinExpression {
        val bridgeArguments = mutableListOf<BridgeTypedKotlinValue>()

        kotlinValues.forEach { (type, value) ->
            if (type.unwrapTypedefs() is RecordType) {
                builder.pushMemScoped()
                val bridgeArgument = "$value.getPointer(memScope).rawValue"
                bridgeArguments.add(BridgeTypedKotlinValue(BridgedType.NATIVE_PTR, bridgeArgument))
            } else {
                val info = mirror(declarationMapper, type).info
                bridgeArguments.add(BridgeTypedKotlinValue(info.bridgedType, info.argToBridged(value)))
            }
        }

        val unwrappedReturnType = returnType.unwrapTypedefs()
        val kniRetVal = "kniRetVal"
        val bridgeReturnType = when (unwrappedReturnType) {
            VoidType -> BridgedType.VOID
            is RecordType -> {
                val mirror = mirror(declarationMapper, returnType)
                val tmpVarName = kniRetVal
                // We clear in the finally block.
                builder.out("val $tmpVarName = nativeHeap.alloc<${mirror.pointedType.render(builder.scope)}>()")
                builder.pushBlock(start = "try {", end = "} finally { nativeHeap.free($tmpVarName) }")
                bridgeArguments.add(BridgeTypedKotlinValue(BridgedType.NATIVE_PTR, "$tmpVarName.rawPtr"))
                BridgedType.VOID
            }
            else -> {
                val mirror = mirror(declarationMapper, returnType)
                mirror.info.bridgedType
            }
        }

        val callExpr = simpleBridgeGenerator.kotlinToNative(
                nativeBacked, bridgeReturnType, bridgeArguments, independent
        ) { bridgeNativeValues ->

            val nativeValues = mutableListOf<String>()
            kotlinValues.forEachIndexed { index, (type, _) ->
                val unwrappedType = type.unwrapTypedefs()
                if (unwrappedType is RecordType) {
                    nativeValues.add("*(${unwrappedType.decl.spelling}*)${bridgeNativeValues[index]}")
                } else {
                    nativeValues.add(
                            mirror(declarationMapper, type).info.cFromBridged(
                                    bridgeNativeValues[index], scope, nativeBacked
                            )
                    )
                }
            }

            val nativeResult = block(nativeValues)

            when (unwrappedReturnType) {
                is VoidType -> {
                    out(nativeResult + ";")
                    ""
                }
                is RecordType -> {
                    val kniStructResult = "kniStructResult"

                    out("${unwrappedReturnType.decl.spelling} $kniStructResult = $nativeResult;")
                    out("memcpy(${bridgeNativeValues.last()}, &$kniStructResult, sizeof($kniStructResult));")
                    ""
                }
                else -> {
                    nativeResult
                }
            }
        }

        val result = when (unwrappedReturnType) {
            is VoidType -> callExpr
            is RecordType -> {
                builder.out(callExpr)
                "$kniRetVal.readValue()"
            }
            else -> {
                val mirror = mirror(declarationMapper, returnType)
                mirror.info.argFromBridged(callExpr, builder.scope, nativeBacked)
            }
        }

        return result
    }

    override fun nativeToKotlin(
            builder: NativeCodeBuilder,
            nativeBacked: NativeBacked,
            returnType: Type,
            nativeValues: List<TypedNativeValue>,
            block: KotlinCodeBuilder.(kotlinValues: List<KotlinExpression>) -> KotlinExpression
    ): NativeExpression {

        val bridgeArguments = mutableListOf<BridgeTypedNativeValue>()

        nativeValues.forEachIndexed { _, (type, value) ->
            val bridgeArgument = if (type.unwrapTypedefs() is RecordType) {
                BridgeTypedNativeValue(BridgedType.NATIVE_PTR, "&$value")
            } else {
                val info = mirror(declarationMapper, type).info
                BridgeTypedNativeValue(info.bridgedType, value)
            }
            bridgeArguments.add(bridgeArgument)
        }

        val unwrappedReturnType = returnType.unwrapTypedefs()
        val kniRetVal = "kniRetVal"
        val bridgeReturnType = when (unwrappedReturnType) {
            VoidType -> BridgedType.VOID
            is RecordType -> {
                val tmpVarName = kniRetVal
                builder.out("${unwrappedReturnType.decl.spelling} $tmpVarName;")
                bridgeArguments.add(BridgeTypedNativeValue(BridgedType.NATIVE_PTR, "&$tmpVarName"))
                BridgedType.VOID
            }
            else -> {
                val mirror = mirror(declarationMapper, returnType)
                mirror.info.bridgedType
            }
        }

        val callExpr = simpleBridgeGenerator.nativeToKotlin(
                nativeBacked,
                bridgeReturnType,
                bridgeArguments
        ) { bridgeKotlinValues ->
            val kotlinValues = mutableListOf<String>()
            nativeValues.forEachIndexed { index, (type, _) ->
                val mirror = mirror(declarationMapper, type)
                if (type.unwrapTypedefs() is RecordType) {
                    val pointedTypeName = mirror.pointedType.render(this.scope)
                    kotlinValues.add(
                            "interpretPointed<$pointedTypeName>(${bridgeKotlinValues[index]}).readValue()"
                    )
                } else {
                    kotlinValues.add(mirror.info.argFromBridged(bridgeKotlinValues[index], this.scope, nativeBacked))
                }
            }

            val kotlinResult = block(kotlinValues)
            when (unwrappedReturnType) {
                is RecordType -> {
                    "$kotlinResult.write(${bridgeKotlinValues.last()})"
                }
                is VoidType -> {
                    kotlinResult
                }
                else -> {
                    mirror(declarationMapper, returnType).info.argToBridged(kotlinResult)
                }
            }
        }

        val result = when (unwrappedReturnType) {
            is VoidType -> callExpr
            is RecordType -> {
                builder.out("$callExpr;")
                kniRetVal
            }
            else -> {
                mirror(declarationMapper, returnType).info.cFromBridged(callExpr, builder.scope, nativeBacked)
            }
        }

        return result
    }
}