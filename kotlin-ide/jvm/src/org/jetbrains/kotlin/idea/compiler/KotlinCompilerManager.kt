/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.compiler

import com.intellij.diagnostic.PluginException
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.config.CompilerRunnerConstants
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import java.io.PrintStream
import java.io.PrintWriter

class KotlinCompilerManager(project: Project, manager: CompilerManager) : ProjectComponent {
    // Extending PluginException ensures that Exception Analyzer recognizes this as a Kotlin exception
    private class KotlinCompilerException(private val text: String) :
        PluginException("", PluginManagerCore.getPluginByClassName(KotlinCompilerManager::class.java.name)) {
        override fun printStackTrace(s: PrintWriter) {
            s.print(text)
        }

        override fun printStackTrace(s: PrintStream) {
            s.print(text)
        }

        @Synchronized
        override fun fillInStackTrace(): Throwable {
            return this
        }

        override fun getStackTrace(): Array<StackTraceElement> {
            LOG.error("Somebody called getStackTrace() on KotlinCompilerException")
            // Return some stack trace that originates in Kotlin
            return UnsupportedOperationException().stackTrace
        }

        override val message: String
            get() = "<Exception from standalone Kotlin compiler>"

    }

    companion object {
        private val LOG = Logger.getInstance(KotlinCompilerManager::class.java)

        // Comes from external make
        private const val PREFIX_WITH_COMPILER_NAME =
            CompilerRunnerConstants.KOTLIN_COMPILER_NAME + ": " + CompilerRunnerConstants.INTERNAL_ERROR_PREFIX
        private val FILE_EXTS_WHICH_NEEDS_REFRESH = ContainerUtil.immutableSet(JavaScript.DOT_EXTENSION, ".map")
    }

    init {
        manager.addCompilableFileType(KotlinFileType.INSTANCE)
        manager.addCompilationStatusListener(object : CompilationStatusListener {
            override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
                for (error in compileContext.getMessages(CompilerMessageCategory.ERROR)) {
                    val message = error.message
                    if (message.startsWith(CompilerRunnerConstants.INTERNAL_ERROR_PREFIX) || message.startsWith(PREFIX_WITH_COMPILER_NAME)) {
                        LOG.error(KotlinCompilerException(message))
                    }
                }
            }

            override fun fileGenerated(outputRoot: String, relativePath: String) {
                if (ApplicationManager.getApplication().isUnitTestMode) return
                val ext = FileUtilRt.getExtension(relativePath).toLowerCase()
                if (FILE_EXTS_WHICH_NEEDS_REFRESH.contains(ext)) {
                    val outFile = "$outputRoot/$relativePath"
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(outFile)
                        ?: error("Virtual file not found for generated file path: $outFile")
                    virtualFile.refresh( /*async =*/false,  /*recursive =*/false)
                }
            }
        }, project)
    }
}