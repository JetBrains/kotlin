package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class GroupByReducePredicate : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver by groupBy()
    val Arguments.predicate by ignore()
    override fun Arguments.interpret(): GroupBy {
        return receiver
    }
}

class GroupByReduceExpression : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver by groupBy()
    val Arguments.rowExpression by ignore()
    override fun Arguments.interpret(): GroupBy {
        return receiver
    }
}

class GroupByReduceInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.columnName: String by arg()
    override fun Arguments.interpret(): PluginDataFrameSchema {
        val group = makeNullable(SimpleColumnGroup(columnName, receiver.groups.columns()))
        return PluginDataFrameSchema(receiver.keys.columns() + group)
    }
}
