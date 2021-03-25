/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import lombok.Getter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

abstract class AbstractLombokCompileTest : CodegenTestCase() {

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compile(files)
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        LombokComponentRegistrar.registerComponents(environment.project, environment.configuration)
        environment.updateClasspath(listOf(JvmClasspathRoot(getLombokJar())))
    }

    override fun updateJavaClasspath(javaClasspath: MutableList<String>) {
        javaClasspath += getLombokJar().absolutePath
    }

    override fun configureTestSpecific(configuration: CompilerConfiguration, testFiles: List<TestFile>) {
        writeLombokConfig(configuration.javaSourceRoots.first(), testFiles)?.let { file ->
            configuration.put(LombokConfigurationKeys.CONFIG_FILE, file)
        }
    }

    private fun writeLombokConfig(directory: String, testFiles: List<TestFile>): File? =
        testFiles.singleOrNull { it.name == "lombok.config" }?.let {
            val file = File(directory, it.name)
            file.writeText(it.content)
            file
        }


    private fun getLombokJar(): File = PathUtil.getResourcePathForClass(Getter::class.java)
}
