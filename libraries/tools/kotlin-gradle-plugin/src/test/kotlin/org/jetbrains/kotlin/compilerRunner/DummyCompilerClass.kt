/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.*

/**
 * A dummy class to be run via in [RunToolInSeparateProcessTest]
 */
@Suppress("DEPRECATION")
class DummyCompilerClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val errors = ArgumentParseErrors()
            val processedArgs = preprocessCommandLineArguments(args.toList(), lazy { errors })
            println("Args size: ${processedArgs.size}")
            processedArgs.forEach {
                println("Arg: $it")
            }
            validateArguments(errors)?.let {
                println("Errors: $it")
                return
            }
            println("OK")
        }
    }
}