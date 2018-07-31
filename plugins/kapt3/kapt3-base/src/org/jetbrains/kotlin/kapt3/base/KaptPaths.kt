/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import java.io.File

class KaptPaths(
    val projectBaseDir: File?,
    compileClasspath: List<File>,
    annotationProcessingClasspath: List<File>,
    val javaSourceRoots: List<File>,
    val sourcesOutputDir: File,
    val classFilesOutputDir: File,
    val stubsOutputDir: File,
    val incrementalDataOutputDir: File?
) {
    val compileClasspath = compileClasspath.distinct()
    val annotationProcessingClasspath = annotationProcessingClasspath.distinct()

    fun collectJavaSourceFiles(): List<File> {
        return (javaSourceRoots + stubsOutputDir)
            .map { it.canonicalFile }
            .distinct()
            .flatMap { root ->
                root.walk().filter { it.isFile && it.extension == "java" }.toList()
            }
    }
}

fun KaptPaths.log(logger: KaptLogger) {
    if (!logger.isVerbose) return

    logger.info("Project base dir: $projectBaseDir")
    logger.info("Compile classpath: ${compileClasspath.joinToString()}")
    logger.info("Annotation processing classpath: ${annotationProcessingClasspath.joinToString()}")
    logger.info("Java source roots: ${javaSourceRoots.joinToString()}")
    logger.info("Sources output directory: $sourcesOutputDir")
    logger.info("Class files output directory: $classFilesOutputDir")
    logger.info("Stubs output directory: $stubsOutputDir")
    logger.info("Incremental data output directory: $incrementalDataOutputDir")
}
