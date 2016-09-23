package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

internal class KotlinJvmOptionsImpl : KotlinJvmOptionsBase() {
    override var freeCompilerArgs: List<String> = listOf()

    override fun updateArguments(args: K2JVMCompilerArguments) {
        super.updateArguments(args)
        K2JVMCompiler().parseArguments(freeCompilerArgs.toTypedArray(), args)
    }
}
