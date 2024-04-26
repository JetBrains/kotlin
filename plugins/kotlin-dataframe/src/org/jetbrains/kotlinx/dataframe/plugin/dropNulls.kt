package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.Marker
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

class DropNulls0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(fillNullsImpl(receiver.columns(), columns.mapTo(mutableSetOf()) { it.path.path }, emptyList()))
    }
}

fun KotlinTypeFacade.fillNullsImpl(
    columns: List<SimpleCol>,
    paths: Set<List<String>>,
    p: List<String>
): List<SimpleCol> {
    return columns.map {
        // else report?
        if (p + it.name() in paths && it.kind() == SimpleColumnKind.VALUE) {
            val coneType = it.type.type as? ConeSimpleKotlinType
            if (coneType != null) {
                val type = coneType.withNullability(ConeNullability.NOT_NULL, session.typeContext)
                it.changeType(Marker.invoke(type))
            } else {
                // report?
                it
            }
        } else {
            if (it is SimpleColumnGroup) {
                val updatedColumns = fillNullsImpl(it.columns(), paths, p + it.name())
                SimpleColumnGroup(it.name(), updatedColumns, anyRow)
            } else {
                it
            }
        }
    }
}

