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
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.kapt3.javac.KaptJavaCompiler
import org.jetbrains.kotlin.kapt3.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import javax.tools.JavaFileManager

class KaptContext<out GState : GenerationState?>(
        val logger: KaptLogger,
        val project: Project,
        val bindingContext: BindingContext,
        val compiledClasses: List<ClassNode>,
        val origins: Map<Any, JvmDeclarationOrigin>,
        val generationState: GState,
        processorOptions: Map<String, String>,
        javacOptions: Map<String, String> = emptyMap()
) : AutoCloseable {
    val context = Context()
    val compiler: KaptJavaCompiler
    val fileManager: JavacFileManager
    val options: Options
    val javaLog: KaptJavaLog

    init {
        KaptJavaLog.preRegister(context, logger.messageCollector)
        JavacFileManager.preRegister(context)
        KaptTreeMaker.preRegister(context, this)
        KaptJavaCompiler.preRegister(context)

        fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
        compiler = JavaCompiler.instance(context) as KaptJavaCompiler

        ClassReader.instance(context).saveParameterNames = true

        javaLog = compiler.log as KaptJavaLog

        options = Options.instance(context)
        for ((key, value) in processorOptions) {
            val option = if (value.isEmpty()) "-A$key" else "-A$key=$value"
            options.put(option, option) // key == value: it's intentional
        }

        for ((key, value) in javacOptions) {
            if (value.isNotEmpty()) {
                options.put(key, value)
            } else {
                options.put(key, key)
            }
        }

        if (logger.isVerbose) {
            logger.info("Javac options: " + options.keySet().keysToMap { key -> options[key] ?: "" })
        }
    }

    override fun close() {
        compiler.close()
        fileManager.close()
        generationState?.destroy()
    }
}