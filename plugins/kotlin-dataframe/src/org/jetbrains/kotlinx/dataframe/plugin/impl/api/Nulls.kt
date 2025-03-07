package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class DropNulls0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(fillNullsImpl(receiver.columns(), columns.resolve(receiver).mapTo(mutableSetOf()) { it.path.path }, emptyList()))
    }
}

class DropNa0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.whereAllNA: Boolean by arg(defaultValue = Present(false))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        if (whereAllNA) return receiver
        return PluginDataFrameSchema(fillNullsImpl(receiver.columns(), columns.resolve(receiver).mapTo(mutableSetOf()) { it.path.path }, emptyList()))
    }
}

fun KotlinTypeFacade.fillNullsImpl(
    columns: List<SimpleCol>,
    paths: Set<List<String>>,
    p: List<String>
): List<SimpleCol> {
    return columns.map {
        // else report?
        if (p + it.name() in paths && it is SimpleDataColumn) {
            val coneType = it.type.type as? ConeSimpleKotlinType
            if (coneType != null) {
                val type = coneType.withNullability(nullable = false, session.typeContext)
                it.changeType(Marker.invoke(type))
            } else {
                // report?
                it
            }
        } else {
            if (it is SimpleColumnGroup) {
                val updatedColumns = fillNullsImpl(it.columns(), paths, p + it.name())
                SimpleColumnGroup(it.name(), updatedColumns)
            } else {
                it
            }
        }
    }
}

