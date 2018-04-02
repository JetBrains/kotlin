package org.konan.calculator

import org.konan.arithmeticparser.*

fun main(args: Array<String>) {
    val expression = if (args.isNotEmpty()) {
        args.first().also {
            println(it)
        }
    } else {
        println("Enter an expression:")
        readLine()!!
    }

    val result = parseAndCompute(expression)
    val computed = result.expression
    if (computed != null) {
        println(" = $computed")
    } else {
        println(" = ${result.partialExpression}")
        result.remainder?.let {
            println("Unable to parse suffix: $it")
        }
    }
}
