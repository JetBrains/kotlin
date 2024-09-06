package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter

class PairConstructor : AbstractInterpreter<Pair<*, *>>() {
    val Arguments.receiver: Any? by arg(lens = Interpreter.Id)
    val Arguments.that: Any? by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): Pair<*, *> {
        return receiver to that
    }
}
