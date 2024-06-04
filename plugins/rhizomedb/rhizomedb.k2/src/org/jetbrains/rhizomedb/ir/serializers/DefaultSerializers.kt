/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir.serializers

import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun CompilerPluginContext.getSpecialSerializer(fqName: FqName?) = remember {
    val listSerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "ListSerializer".name
    )).first()

    val setSerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "SetSerializer".name
    )).first()

    val mapSerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "MapSerializer".name
    )).first()

    val pairSerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "PairSerializer".name
    )).first()

    val mapEntrySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "MapEntrySerializer".name
    )).first()

    val tripleSerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "TripleSerializer".name
    )).first()

    val charArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "CharArraySerializer".name
    )).first()

    val byteArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "ByteArraySerializer".name
    )).first()

    val uByteArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "UByteArraySerializer".name
    )).first()

    val shortArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "ShortArraySerializer".name
    )).first()

    val uShortArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "UShortArraySerializer".name
    )).first()

    val intArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "IntArraySerializer".name
    )).first()

    val uIntArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "UIntArraySerializer".name
    )).first()

    val longArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "LongArraySerializer".name
    )).first()

    val uLongArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "ULongArraySerializer".name
    )).first()

    val floatArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "FloatArraySerializer".name
    )).first()

    val doubleArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "DoubleArraySerializer".name
    )).first()

    val booleanArraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "BooleanArraySerializer".name
    )).first()

    val arraySerializer = pluginContext.referenceFunctions(CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "serialization", "builtins")),
        className = null,
        callableName = "ArraySerializer".name
    )).first { it.owner.valueParameters.size == 1 }

    mapOf(
        LIST_FQN to listSerializer,
        SET_FQN to setSerializer,
        MAP_FQN to mapSerializer,
        PAIR_FQN to pairSerializer,
        MAP_ENTRY_FQN to mapEntrySerializer,
        TRIPLE_FQN to tripleSerializer,
        CHAR_ARRAY_FQN to charArraySerializer,
        BYTE_ARRAY_FQN to byteArraySerializer,
        U_BYTE_ARRAY_FQN to uByteArraySerializer,
        SHORT_ARRAY_FQN to shortArraySerializer,
        U_SHORT_ARRAY_FQN to uShortArraySerializer,
        INT_ARRAY_FQN to intArraySerializer,
        U_INT_ARRAY_FQN to uIntArraySerializer,
        LONG_ARRAY_FQN to longArraySerializer,
        U_LONG_ARRAY_FQN to uLongArraySerializer,
        FLOAT_ARRAY_FQN to floatArraySerializer,
        DOUBLE_ARRAY_FQN to doubleArraySerializer,
        BOOLEAN_ARRAY_FQN to booleanArraySerializer,
        ARRAY_FQN to arraySerializer,
    )
}[fqName]


private val LIST_FQN = FqName.fromSegments(listOf("kotlin", "collections", "List"))
private val SET_FQN = FqName.fromSegments(listOf("kotlin", "collections", "Set"))
private val MAP_FQN = FqName.fromSegments(listOf("kotlin", "collections", "Map"))
private val PAIR_FQN = FqName.fromSegments(listOf("kotlin", "Pair"))
private val MAP_ENTRY_FQN = FqName.fromSegments(listOf("kotlin", "collections", "Map", "Entry"))
private val TRIPLE_FQN = FqName.fromSegments(listOf("kotlin", "Triple"))
private val CHAR_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "CharArray"))
private val BYTE_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "ByteArray"))
private val U_BYTE_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "UByteArray"))
private val SHORT_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "ShortArray"))
private val U_SHORT_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "UShortArray"))
private val INT_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "IntArray"))
private val U_INT_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "UIntArray"))
private val LONG_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "LongArray"))
private val U_LONG_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "ULongArray"))
private val FLOAT_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "FloatArray"))
private val DOUBLE_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "DoubleArray"))
private val BOOLEAN_ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "BooleanArray"))
private val ARRAY_FQN = FqName.fromSegments(listOf("kotlin", "Array"))
