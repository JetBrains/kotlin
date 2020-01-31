/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.stm.compiler.extensions.StmComponentRegistrar
import java.io.File

abstract class AbstractStmIrBytecodeListingTest : AbstractAsmLikeInstructionListingTest() {
    override fun getExpectedTextFileName(wholeFile: File): String {
        return wholeFile.nameWithoutExtension + ".ir.txt"
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        StmComponentRegistrar.registerExtensions(environment.project)
        val t = PathUtil.getResourcePathForClass(Class.forName("kotlinx.stm.SharedMutable"))
        environment.updateClasspath(listOf(JvmClasspathRoot(t)))
    }
}