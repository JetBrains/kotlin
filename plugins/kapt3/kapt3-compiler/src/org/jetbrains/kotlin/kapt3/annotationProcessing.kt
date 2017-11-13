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

import com.sun.tools.javac.comp.CompileStates.*
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.processing.AnnotationProcessingError
import com.sun.tools.javac.processing.JavacFiler
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.util.putJavacOption
import java.io.File
import javax.annotation.processing.Processor
import javax.tools.JavaFileObject
import com.sun.tools.javac.util.List as JavacList

fun KaptContext<*>.doAnnotationProcessing(
        javaSourceFiles: List<File>,
        processors: List<Processor>,
        compileClasspath: List<File>,
        annotationProcessingClasspath: List<File>,
        annotationProcessors: String,
        sourcesOutputDir: File,
        classesOutputDir: File,
        additionalSources: JavacList<JCTree.JCCompilationUnit> = JavacList.nil(),
        withJdk: Boolean = false
) {
    with (options) {
        put(Option.PROC, "only") // Only process annotations

        if (!withJdk) {
            putJavacOption("BOOTCLASSPATH", "BOOT_CLASS_PATH", "") // No boot classpath
        }

        putJavacOption("CLASSPATH", "CLASS_PATH", compileClasspath.joinToString(File.pathSeparator) { it.canonicalPath })
        putJavacOption("PROCESSORPATH", "PROCESSOR_PATH", annotationProcessingClasspath.joinToString(File.pathSeparator) { it.canonicalPath })
        put(Option.PROCESSOR, annotationProcessors)
        put(Option.S, sourcesOutputDir.canonicalPath)
        put(Option.D, classesOutputDir.canonicalPath)
        put(Option.ENCODING, "UTF-8")
    }

    val processingEnvironment = JavacProcessingEnvironment.instance(context)

    val compilerAfterAP: JavaCompiler
    try {
        if (isJava9OrLater) {
            val initProcessAnnotationsMethod = JavaCompiler::class.java.declaredMethods.single { it.name == "initProcessAnnotations" }
            initProcessAnnotationsMethod.invoke(compiler, processors, emptyList<JavaFileObject>(), emptyList<String>())
        }
        else {
            compiler.initProcessAnnotations(processors)
        }

        val parsedJavaFiles = parseJavaFiles(javaSourceFiles)

        compilerAfterAP = try {
            javaLog.interceptorData.files = parsedJavaFiles.map { it.sourceFile to it }.toMap()
            val analyzedFiles = compiler.stopIfErrorOccurred(
                    CompileState.PARSE, compiler.enterTrees(parsedJavaFiles + additionalSources))

            if (isJava9OrLater) {
                val processAnnotationsMethod = compiler.javaClass.getMethod("processAnnotations", JavacList::class.java)
                processAnnotationsMethod.invoke(compiler, analyzedFiles)
                compiler
            }
            else {
                compiler.processAnnotations(analyzedFiles)
            }
        } catch (e: AnnotationProcessingError) {
            throw KaptError(KaptError.Kind.EXCEPTION, e.cause ?: e)
        }

        val log = compilerAfterAP.log

        val filer = processingEnvironment.filer as JavacFiler
        val errorCount = log.nerrors
        val warningCount = log.nwarnings

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

internal fun KaptContext<*>.parseJavaFiles(javaSourceFiles: List<File>): JavacList<JCTree.JCCompilationUnit> {
    val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)

    return compiler.stopIfErrorOccurred(CompileState.PARSE,
                    initModulesIfNeeded(
                            compiler.stopIfErrorOccurred(CompileState.PARSE,
                                    compiler.parseFiles(javaFileObjects))))
}

private fun KaptContext<*>.initModulesIfNeeded(files: JavacList<JCTree.JCCompilationUnit>): JavacList<JCTree.JCCompilationUnit> {
    if (isJava9OrLater) {
        val initModulesMethod = compiler.javaClass.getMethod("initModules", JavacList::class.java)

        @Suppress("UNCHECKED_CAST")
        return compiler.stopIfErrorOccurred(
                CompileState.PARSE,
                initModulesMethod.invoke(compiler, files) as JavacList<JCTree.JCCompilationUnit>)
    }

    return files
}