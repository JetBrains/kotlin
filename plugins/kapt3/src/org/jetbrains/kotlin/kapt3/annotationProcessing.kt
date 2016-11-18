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
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.processing.AnnotationProcessingError
import com.sun.tools.javac.processing.JavacFiler
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import java.io.File
import javax.annotation.processing.Processor
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList

fun KaptContext.doAnnotationProcessing(
        javaSourceFiles: List<File>,
        processors: List<Processor>,
        classpath: List<File>,
        sourcesOutputDir: File,
        classesOutputDir: File,
        additionalSources: JavacList<JCTree.JCCompilationUnit> = JavacList.nil()
) {
    with (options) {
        put(Option.PROC, "only") // Only process annotations
        put(Option.CLASSPATH, classpath.joinToString(File.pathSeparator) { it.canonicalPath })
        put(Option.S, sourcesOutputDir.canonicalPath)
        put(Option.D, classesOutputDir.canonicalPath)
    }

    val fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
    val processingEnvironment = JavacProcessingEnvironment.instance(context)

    try {
        compiler.initProcessAnnotations(processors)

        val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)
        val parsedJavaFiles = compiler.parseFiles(javaFileObjects)

        val log = Log.instance(context)
        if (compiler.shouldStop(CompileStates.CompileState.PARSE) || log.nerrors > 0) {
            throw KaptError(KaptError.Kind.JAVA_FILE_PARSING_ERROR)
        }

        val warningsBeforeAp: Int
        try {
            val analyzedFiles = compiler.enterTrees(parsedJavaFiles + additionalSources)
            if (log.nerrors > 0) {
                throw KaptError(KaptError.Kind.ERROR_WHILE_ANALYSIS)
            }

            warningsBeforeAp = log.nwarnings

            compiler.processAnnotations(analyzedFiles)
        } catch (e: AnnotationProcessingError) {
            throw KaptError(KaptError.Kind.EXCEPTION, e.cause ?: e)
        }

        val filer = processingEnvironment.filer as JavacFiler
        val errorCount = log.nerrors
        val warningCount = log.nwarnings - warningsBeforeAp

        logger.info { "Annotation processing complete, errors: $errorCount, warnings: $warningCount" }
        if (logger.isVerbose) {
            filer.displayState()
        }

        if (log.nerrors > 0) {
            throw KaptError(KaptError.Kind.ERROR_RAISED)
        }
    } finally {
        processingEnvironment.close()
        this@doAnnotationProcessing.close()
    }
}