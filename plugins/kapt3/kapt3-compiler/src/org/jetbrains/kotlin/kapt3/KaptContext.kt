/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3

import com.intellij.openapi.project.Project
import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.kapt3.javac.KaptJavaCompiler
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileManager
import org.jetbrains.kotlin.kapt3.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.kapt3.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.util.putJavacOption
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File
import javax.tools.JavaFileManager

class KaptContext<out GState : GenerationState?>(
    val paths: KaptPaths,
    private val withJdk: Boolean,
    aptMode: AptMode,
    val logger: KaptLogger,
    val project: Project,
    val bindingContext: BindingContext,
    val compiledClasses: List<ClassNode>,
    val origins: Map<Any, JvmDeclarationOrigin>,
    val generationState: GState,
    mapDiagnosticLocations: Boolean,
    processorOptions: Map<String, String>,
    javacOptions: Map<String, String> = emptyMap()
) : AutoCloseable {
    val context = Context()
    val compiler: KaptJavaCompiler
    val fileManager: KaptJavaFileManager
    val options: Options
    val javaLog: KaptJavaLog
    private val treeMaker: TreeMaker

    init {
        KaptJavaLog.preRegister(this, logger.messageCollector, mapDiagnosticLocations)
        KaptJavaFileManager.preRegister(context)
        if (aptMode != AptMode.APT_ONLY) {
            KaptTreeMaker.preRegister(context, this)
        }
        KaptJavaCompiler.preRegister(context)

        options = Options.instance(context).apply {
            for ((key, value) in processorOptions) {
                val option = if (value.isEmpty()) "-A$key" else "-A$key=$value"
                put(option, option) // key == value: it's intentional
            }

            for ((key, value) in javacOptions) {
                if (value.isNotEmpty()) {
                    put(key, value)
                } else {
                    put(key, key)
                }
            }

            put(Option.PROC, "only") // Only process annotations

            if (!withJdk) {
                putJavacOption("BOOTCLASSPATH", "BOOT_CLASS_PATH", "") // No boot classpath
            }

            if (isJava9OrLater) {
                put("accessInternalAPI", "true")
            }

            putJavacOption("CLASSPATH", "CLASS_PATH",
                           paths.compileClasspath.joinToString(File.pathSeparator) { it.canonicalPath })
            putJavacOption("PROCESSORPATH", "PROCESSOR_PATH",
                           paths.annotationProcessingClasspath.joinToString(File.pathSeparator) { it.canonicalPath })

            put(Option.S, paths.sourcesOutputDir.canonicalPath)
            put(Option.D, paths.classFilesOutputDir.canonicalPath)
            put(Option.ENCODING, "UTF-8")
        }

        if (logger.isVerbose) {
            logger.info("Javac options: " + options.keySet().keysToMap { key -> options[key] ?: "" })
        }

        fileManager = context.get(JavaFileManager::class.java) as KaptJavaFileManager

        if (isJava9OrLater) {
            for (option in Option.getJavacFileManagerOptions()) {
                val value = options.get(option) ?: continue
                fileManager.handleOptionJavac9(option, value)
            }
        }

        compiler = JavaCompiler.instance(context) as KaptJavaCompiler
        compiler.keepComments = true

        ClassReader.instance(context).saveParameterNames = true

        javaLog = compiler.log as KaptJavaLog
        treeMaker = TreeMaker.instance(context)
    }

    override fun close() {
        (treeMaker as? KaptTreeMaker)?.dispose()
        compiler.close()
        fileManager.close()
        generationState?.destroy()
    }
}