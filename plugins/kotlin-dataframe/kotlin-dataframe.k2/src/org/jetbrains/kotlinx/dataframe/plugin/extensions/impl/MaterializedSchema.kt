/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions.impl

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn

class MaterializedSchema internal constructor(val name: String, val schema: PluginDataFrameSchema, val asDataClass: Boolean)

context(_: SessionHolder)
fun PluginDataFrameSchema.toMaterializedSchema(name: String, asDataClass: Boolean): MaterializedSchema {
    return MaterializedSchema(name, PluginDataFrameSchema(columns().approximateTypes()), asDataClass)
}

context(sessionHolder: SessionHolder)
private fun List<SimpleCol>.approximateTypes(): List<SimpleCol> {
    return map {
        when (it) {
            is SimpleColumnGroup -> SimpleColumnGroup(it.name, it.columns().approximateTypes())
            is SimpleFrameColumn -> SimpleFrameColumn(it.name, it.columns().approximateTypes())
            is SimpleDataColumn -> {
                val denotable = sessionHolder.session.typeApproximator.approximateToSuperType(
                    it.type.coneType,
                    TypeApproximatorConfiguration.PublicDeclaration.ApproximateLocalAndAnonymousTypes
                ) ?: it.type.coneType
                SimpleDataColumn(it.name, type = denotable.wrap())
            }
        }
    }
}
