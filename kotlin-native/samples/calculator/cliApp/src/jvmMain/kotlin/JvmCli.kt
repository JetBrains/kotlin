/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("JvmCli")

package sample.calculator.jvm

import sample.calculator.arithmeticparser.parseAndCompute

fun main(args: Array<String>) {
    val expression = if (args.isNotEmpty()) {
        args.first().also {
            print(it)
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
