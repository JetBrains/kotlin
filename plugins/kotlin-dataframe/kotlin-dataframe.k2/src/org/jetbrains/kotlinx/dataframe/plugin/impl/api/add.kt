package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

typealias TypeApproximation = Marker

class Add : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.name: String by arg()
    val Arguments.infer by ignore()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(receiver.columns() + simpleColumnOf(name, type.coneType))
    }
}

class From : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret() {
        dsl.columns += simpleColumnOf(receiver, type.coneType)
    }
}

class Into : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: TypeApproximation by type()
    val Arguments.name: String by arg()

    override fun Arguments.interpret() {
        val valuesType = extractBaseColumnValuesType(receiver.coneType) ?: session.builtinTypes.nullableAnyType.coneType
        dsl.columns += simpleColumnOf(name, valuesType)
    }
}

class AddDslApproximation(val columns: MutableList<SimpleCol>)

class AddWithDsl : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.body by dsl()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val addDsl = AddDslApproximation(receiver.columns().toMutableList())
        body(addDsl, emptyMap())
        return PluginDataFrameSchema(addDsl.columns)
    }
}

class AddDslStringInvoke : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.body by dsl()

    override fun Arguments.interpret() {
        val addDsl = AddDslApproximation(mutableListOf())
        body(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(receiver, addDsl.columns))
    }
}

class AddDslNamedGroup : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.name: String by arg()
    val Arguments.body by dsl()

    override fun Arguments.interpret() {
        val addDsl = AddDslApproximation(mutableListOf())
        body(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(name, addDsl.columns))
    }
}

class AddDslAddGroup : AbstractInterpreter<AddDslApproximation>() {
    val Arguments.body by dsl()

    override fun Arguments.interpret(): AddDslApproximation {
        val addDsl = AddDslApproximation(mutableListOf())
        body(addDsl, emptyMap())
        return addDsl
    }
}

class AddDslAddGroupInto : AbstractInterpreter<Unit>() {
    val Arguments.dsl: AddDslApproximation by arg()
    val Arguments.receiver: AddDslApproximation by arg()
    val Arguments.groupName: String by arg()

    override fun Arguments.interpret() {
        dsl.columns.add(SimpleColumnGroup(groupName, receiver.columns))
    }
}