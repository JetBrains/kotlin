package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class DropNulls0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.whereAllNull: Boolean by arg(defaultValue = Present(false))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        if (whereAllNull) return receiver
        return PluginDataFrameSchema(
            fillNullsImpl(
                receiver.columns(),
                columns.resolve(receiver).mapTo(mutableSetOf()) { it.path.path },
                emptyList()
            )
        )
    }
}

class DropNulls1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.whereAllNull: Boolean by arg(defaultValue = Present(false))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        if (whereAllNull) return receiver
        return PluginDataFrameSchema(
            fillNullsImpl(
                receiver.columns(),
                receiver.columns().mapToSetOrEmpty { listOf(it.name) },
                emptyList()
            )
        )
    }
}

class DropNa0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.whereAllNA: Boolean by arg(defaultValue = Present(false))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        if (whereAllNA) return receiver
        return PluginDataFrameSchema(
            fillNullsImpl(
                receiver.columns(),
                columns.resolve(receiver).mapTo(mutableSetOf()) { it.path.path },
                emptyList()
            )
        )
    }
}

class DropNa1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.whereAllNA: Boolean by arg(defaultValue = Present(false))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        if (whereAllNA) return receiver
        return PluginDataFrameSchema(
            fillNullsImpl(
                receiver.columns(),
                receiver.columns().mapToSetOrEmpty { listOf(it.name) },
                emptyList()
            )
        )
    }
}

fun KotlinTypeFacade.fillNullsImpl(
    columns: List<SimpleCol>,
    paths: Set<List<String>>,
    p: List<String>,
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

