/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.console

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.KotlinClassPath
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.JavaParametersBuilder
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.subplatformsOfType
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class KotlinConsoleKeeper(val project: Project) {
    private val consoleMap: MutableMap<VirtualFile, KotlinConsoleRunner> = ConcurrentHashMap()

    fun getConsoleByVirtualFile(virtualFile: VirtualFile) = consoleMap[virtualFile]
    fun putVirtualFileToConsole(virtualFile: VirtualFile, console: KotlinConsoleRunner) = consoleMap.put(virtualFile, console)
    fun removeConsole(virtualFile: VirtualFile) = consoleMap.remove(virtualFile)

    fun run(module: Module, previousCompilationFailed: Boolean = false): KotlinConsoleRunner? {
        val path = module.moduleFilePath
        val cmdLine = createReplCommandLine(project, module)
        val consoleRunner = KotlinConsoleRunner(
            module,
            cmdLine,
            previousCompilationFailed,
            project,
            KotlinIdeaReplBundle.message("name.kotlin.repl"),
            path
        )

        consoleRunner.initAndRun()
        return consoleRunner
    }

    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.console")

        @JvmStatic
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinConsoleKeeper::class.java)

        fun createReplCommandLine(project: Project, module: Module?): GeneralCommandLine {
            val javaParameters = JavaParametersBuilder(project)
                .withSdkFrom(module, true)
                .withMainClassName("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                .build()

            javaParameters.charset = null
            javaParameters.vmParametersList.add("-Dkotlin.repl.ideMode=true")

            val kotlinArtifacts = KotlinArtifacts.getInstance()
            javaParameters.classPath.apply {
                val classPath = KotlinClassPath.CompilerWithScripting.computeClassPath(kotlinArtifacts)
                addAll(classPath.map {
                    val absolutePath = it.absolutePath
                    if (!it.exists()) {
                        LOG.warn("Compiler dependency classpath $absolutePath does not exist")
                    }
                    absolutePath
                })
            }

            if (module != null) {
                val classPath = JavaParametersBuilder.getModuleDependencies(module)
                if (classPath.isNotEmpty()) {
                    javaParameters.setUseDynamicParameters(javaParameters.isDynamicClasspath)
                    javaParameters.programParametersList.add("-cp")
                    javaParameters.programParametersList.add(
                        classPath.joinToString(File.pathSeparator)
                    )
                }
                TargetPlatformDetector.getPlatform(module).subplatformsOfType<JdkPlatform>().firstOrNull()?.targetVersion?.let {
                    javaParameters.programParametersList.add("-jvm-target")
                    javaParameters.programParametersList.add(it.description)
                }
            }

            javaParameters.programParametersList.add("-kotlin-home")
            javaParameters.programParametersList.add(kotlinArtifacts.kotlincDirectory.also {
                check(it.exists()) {
                    "Kotlinc directory does not exist"
                }
            }.absolutePath)

            return javaParameters.toCommandLine()
        }
    }
}
