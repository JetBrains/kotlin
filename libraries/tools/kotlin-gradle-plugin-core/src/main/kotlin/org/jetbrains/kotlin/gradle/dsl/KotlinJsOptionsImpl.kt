package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler

class KotlinJsOptionsImpl() : KotlinJsOptionsBase() {
    override var freeCompilerArgs: List<String> = listOf()

    override fun updateArguments(args: K2JSCompilerArguments) {
        super.updateArguments(args)
        K2JSCompiler().parseArguments(freeCompilerArgs.toTypedArray(), args)
    }
}
