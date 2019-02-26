/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.repl.ReplFromTerminal

class JvmCliReplShellExtension : ShellExtension {

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean = true

    override fun run(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        projectEnvironment: JavaCoreProjectEnvironment
    ): ExitCode {
        ReplFromTerminal.run(projectEnvironment.parentDisposable, configuration)
        return ExitCode.OK
    }
}