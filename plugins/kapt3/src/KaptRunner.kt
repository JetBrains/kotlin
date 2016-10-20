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

import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import java.io.File
import java.io.PrintWriter
import javax.annotation.processing.Processor
import javax.tools.JavaFileManager

class KaptError : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class KaptRunner {
    fun doAnnotationProcessing(javaSourceFiles: List<File>, processors: List<Processor>, sourceOutputDir: File, classOutputDir: File) {
        val context = Context()
        JavacFileManager.preRegister(context)
        KaptJavaCompiler.preRegister(context)

        val options = Options.instance(context)
        context.put(Log.outKey, PrintWriter(System.err, true))
        options.put(Option.PROC, "only") // Only process annotations
        options.put(Option.S, sourceOutputDir.canonicalPath)
        options.put(Option.D, classOutputDir.canonicalPath)

        val fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
        val compiler = JavaCompiler.instance(context) as KaptJavaCompiler
        val processingEnvironment = JavacProcessingEnvironment.instance(context)

        try {
            compiler.initProcessAnnotations(processors)

            val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)
            val parsedJavaFiles = compiler.parseFiles(javaFileObjects)
            if (compiler.shouldStop(CompileStates.CompileState.PARSE)) {
                throw KaptError("Error while parsing Java files.")
            }

            val compilerAfterAnnotationProcessing = compiler.processAnnotations(compiler.enterTrees(parsedJavaFiles))
            compilerAfterAnnotationProcessing.close()
        } finally {
            processingEnvironment.close()
            fileManager.close()
        }
    }
}