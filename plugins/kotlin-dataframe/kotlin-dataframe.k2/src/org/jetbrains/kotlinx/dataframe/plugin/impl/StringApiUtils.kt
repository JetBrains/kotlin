/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlin.utils.mapToSetOrEmpty

/**
 * If column not in the schema but String API refers to it, operation will fail with runtime exception
 * This allows us to sort of "smart cast" the schema
 *  However, only with `Any?` type
 */
context(arguments: Arguments)
fun PluginDataFrameSchema.createImpliedColumns(selector: List<String>): PluginDataFrameSchema {
    val nullableAny = arguments.session.builtinTypes.nullableAnyType.coneType
    val topLevelColumns = columns().mapToSetOrEmpty { it.name }
    val assumedColumns = selector.filter { it !in topLevelColumns }
        .map { simpleColumnOf(it, nullableAny) }
    return PluginDataFrameSchema(columns() + assumedColumns)
}