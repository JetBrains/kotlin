package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present

class PairToConstructor : AbstractInterpreter<Pair<*, *>>() {
    val Arguments.receiver: Any? by arg(lens = Interpreter.Id)
    val Arguments.that: Any? by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): Pair<*, *> {
        return receiver to that
    }
}

class PairConstructor : AbstractInterpreter<Pair<*, *>>() {
    val Arguments.first: Any? by arg(lens = Interpreter.Id)
    val Arguments.second: Any? by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): Pair<*, *> {
        return first to second
    }
}

class TrimMargin : AbstractInterpreter<String>() {
    val Arguments.receiver: String by arg()
    val Arguments.marginPrefix: String by arg(defaultValue = Present("|"))

    override fun Arguments.interpret(): String {
        return receiver.trimMargin(marginPrefix)
    }
}

class TrimIndent : AbstractInterpreter<String>() {
    val Arguments.receiver: String by arg()

    override fun Arguments.interpret(): String {
        return receiver.trimIndent()
    }
}

