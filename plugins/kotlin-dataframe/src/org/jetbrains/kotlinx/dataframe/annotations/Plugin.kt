package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.string
import org.jetbrains.kotlinx.dataframe.plugin.type

typealias TypeApproximation = org.jetbrains.kotlinx.dataframe.Marker

class ConvertApproximation(val schema: PluginDataFrameSchema, val columns: List<List<String>>)

class Add : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.name: String by string()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(receiver.columns() + SimpleCol(name, type))
    }
}

class Add1 : AbstractSchemaModificationInterpreter() {

    val Arguments.name: String by string()
    val Arguments.expression: TypeApproximation by type()
    val Arguments.parent: String by string()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(listOf(SimpleCol(name, expression)))
    }
}

class From : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg(lens = Interpreter.Value)
    val Arguments.receiver: String by string()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret() {
        dsl.columns += SimpleCol(receiver, type)
    }
}

class Into : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg(lens = Interpreter.Value)
    val Arguments.receiver: TypeApproximation by type()
    val Arguments.name: String by string()

    override fun Arguments.interpret() {
        dsl.columns += SimpleCol(name, receiver)
    }
}


class AddDslApproximation(val columns: MutableList<SimpleCol>)

class AddWithDsl : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.body: (Any) -> Unit by arg(lens = Interpreter.Dsl)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val addDsl = AddDslApproximation(receiver.columns().toMutableList())
        body(addDsl)
        return PluginDataFrameSchema(addDsl.columns)
    }
}
