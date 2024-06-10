package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.string
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

typealias TypeApproximation = Marker

class Add : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.name: String by string()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(receiver.columns() + SimpleCol(name, type))
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
