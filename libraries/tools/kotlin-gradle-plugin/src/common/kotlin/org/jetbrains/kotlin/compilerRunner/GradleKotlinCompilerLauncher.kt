/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.api.CompilerConfiguration
import org.jetbrains.kotlin.api.KotlinCompilerLauncher
import javax.inject.Inject

class GradleKotlinCompilerLauncher(private val execOps: ExecOperations) : KotlinCompilerLauncher {
    override fun launch(compilerConfiguration: CompilerConfiguration) {
        execOps.javaexec {
            it.mainClass.set(compilerConfiguration.mainClassName)
            it.classpath(compilerConfiguration.classpath)
            it.jvmArgs = compilerConfiguration.jvmArguments
        }
    }
}