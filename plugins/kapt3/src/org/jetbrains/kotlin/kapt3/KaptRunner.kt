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
import com.sun.tools.javac.processing.AnnotationProcessingError
import com.sun.tools.javac.processing.JavacFiler
import com.sun.tools.javac.processing.JavacMessager
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import java.io.File
import javax.annotation.processing.Processor
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList

class KaptError : RuntimeException {
    val kind: Kind

    enum class Kind(val message: String) {
        JAVA_FILE_PARSING_ERROR("Java file parsing error"),
        EXCEPTION("Exception while annotation processing"),
        ERROR_RAISED("Error while annotation processing"),
        UNKNOWN("Unknown error while annotation processing")
    }

    constructor(kind: Kind) : super(kind.message) {
        this.kind = kind
    }

    constructor(kind: Kind, cause: Throwable) : super(kind.message, cause) {
        this.kind = kind
    }
}

class KaptRunner(val logger: Logger) {
    val context = Context()
    val compiler: KaptJavaCompiler
    val fileManager: JavacFileManager
    private val options: Options

    init {
        JavacFileManager.preRegister(context)
        KaptTreeMaker.preRegister(context)
        KaptJavaCompiler.preRegister(context)

        fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
        compiler = JavaCompiler.instance(context) as KaptJavaCompiler

        options = Options.instance(context)
    }

    fun close() {
        compiler.close()
        fileManager.close()
    }

    fun parseJavaFiles(
            javaSourceFiles: List<File>,
            classpath: List<File>
    ): JavacList<JCTree.JCCompilationUnit> {
        classpath.forEach { options.put(Option.CLASSPATH, it.canonicalPath) }

        val fileManager = context.get(JavaFileManager::class.java) as JavacFileManager

        try {
            val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)
            return compiler.parseFiles(javaFileObjects)
        } finally {
            fileManager.close()
            compiler.close()
        }
    }

    fun doAnnotationProcessing(
            javaSourceFiles: List<File>,
            processors: List<Processor>,
            classpath: List<File>,
            sourcesOutputDir: File,
            classesOutputDir: File,
            additionalSources: JavacList<JCTree.JCCompilationUnit> = JavacList.nil()
    ) {
        options.put(Option.PROC, "only") // Only process annotations
        options.put(Option.CLASSPATH, classpath.joinToString(File.pathSeparator) { it.canonicalPath })
        options.put(Option.S, sourcesOutputDir.canonicalPath)
        options.put(Option.D, classesOutputDir.canonicalPath)

        val fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
        val processingEnvironment = JavacProcessingEnvironment.instance(context)

        try {
            compiler.initProcessAnnotations(processors)

            val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)
            val parsedJavaFiles = compiler.parseFiles(javaFileObjects)
            if (compiler.shouldStop(CompileStates.CompileState.PARSE)) {
                throw KaptError(KaptError.Kind.JAVA_FILE_PARSING_ERROR)
            }

            val log = Log.instance(context)
            val errorCountBeforeAp = log.nerrors
            val warningsBeforeAp = log.nwarnings

            val compilerAfterAnnotationProcessing: JavaCompiler? = null
            try {
                compiler.processAnnotations(compiler.enterTrees(parsedJavaFiles + additionalSources))
            } catch (e: AnnotationProcessingError) {
                throw KaptError(KaptError.Kind.EXCEPTION, e.cause ?: e)
            }

            val filer = processingEnvironment.filer as JavacFiler
            val errorCount = Math.max(log.nerrors, errorCountBeforeAp)
            val warningCount = log.nwarnings - warningsBeforeAp

            logger.info { "Annotation processing complete, errors: $errorCount, warnings: $warningCount" }
            if (logger.isVerbose) {
                filer.displayState()
            }

            if (log.nerrors > 0) {
                throw KaptError(KaptError.Kind.ERROR_RAISED)
            }

            compilerAfterAnnotationProcessing?.close()
        } finally {
            processingEnvironment.close()
            compiler.close()
            fileManager.close()
        }
    }
}