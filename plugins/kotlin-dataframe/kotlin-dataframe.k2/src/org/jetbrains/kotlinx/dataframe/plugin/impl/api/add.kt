package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

class Add : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.name: String by arg()
    val Arguments.infer by ignore()
    val Arguments.type: ColumnType by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(receiver.columns() + simpleColumnOf(name, type.coneType))
    }
}

class From : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.type: ColumnType by type(name("expression"))

    override fun Arguments.interpret() {
        dsl.columns += simpleColumnOf(receiver, type.coneType)
    }
}

class Into : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: ColumnType by type()
    val Arguments.name: String by arg()

    override fun Arguments.interpret() {
        val valuesType = extractBaseColumnValuesType(receiver.coneType) ?: StandardTypes.NullableAny
        dsl.columns += simpleColumnOf(name, valuesType)
    }
}

class AddDslReferencePlus : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: ResolvedDataColumn by arg()

    override fun Arguments.interpret() {
        dsl.columns += receiver.col.column
    }
}

class AddDslStringPlus : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()

    override fun Arguments.interpret() {
        val column = dsl.originalColumns.firstOrNull { it.name == receiver }
            ?: error("Column '$receiver' is not found in ${dsl.originalColumns.map { it.name }}")
        dsl.columns += column
    }
}

class AddDslFromString : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.column: ResolvedDataColumn by arg()

    override fun Arguments.interpret() {
        dsl.columns += column.col.column.rename(receiver)
    }
}

class AddDslExpr : AbstractInterpreter<ResolvedDataColumn>() {
    val Arguments.infer by ignore()
    val Arguments.expression: ColumnType by type()

    override fun Arguments.interpret(): ResolvedDataColumn {
        return ResolvedDataColumn(
            ColumnWithPathApproximation(
                ColumnPath(listOf("")),
                simpleColumnOf("", expression.coneType)
            )
        )
    }
}

/**
 * @param originalColumns columns of the receiver DataFrame. Nested group DSLs share it with the outer DSL,
 * because the runtime `AddDsl` of a group is created for the same DataFrame.
 */
class AddDslApproximation(
    val columns: MutableList<SimpleCol>,
    val originalColumns: List<SimpleCol>,
)

class AddWithDsl : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.body by dsl()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val addDsl = AddDslApproximation(receiver.columns().toMutableList(), receiver.columns())
        body(addDsl, emptyMap())
        return PluginDataFrameSchema(addDsl.columns)
    }
}

class AddDslStringInvoke : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.body by dsl()

    override fun Arguments.interpret() {
        val addDsl = AddDslApproximation(mutableListOf(), dsl.originalColumns)
        body(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(receiver, addDsl.columns))
    }
}

class AddDslNamedGroup : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.name: String by arg()
    val Arguments.body by dsl()

    override fun Arguments.interpret() {
        val addDsl = AddDslApproximation(mutableListOf(), dsl.originalColumns)
        body(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(name, addDsl.columns))
    }
}

/**
 * `group { ... }` is interpreted as a nested call of `into`'s receiver, without access to the outer DSL,
 * so the body is replayed later by [AddDslAddGroupInto] against the outer DSL's original columns.
 */
class AddGroupApproximation(val body: (Any, Map<String, Interpreter.Success<Any?>>) -> Unit)

class AddDslAddGroup : AbstractInterpreter<AddGroupApproximation>() {
    val Arguments.body by dsl()

    override fun Arguments.interpret(): AddGroupApproximation {
        return AddGroupApproximation(body)
    }
}

class AddDslAddGroupInto : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: AddGroupApproximation by arg()
    val Arguments.groupName: String by arg()

    override fun Arguments.interpret() {
        val addDsl = AddDslApproximation(mutableListOf(), dsl.originalColumns)
        receiver.body(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(groupName, addDsl.columns))
    }
}
