/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun addParcelizeRuntimeLibrary(environment: KotlinCoreEnvironment) {
    val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "parcelize-runtime.jar")
    environment.updateClasspath(listOf(JvmClasspathRoot(runtimeLibrary)))
}

fun addAndroidJarLibrary(environment: KotlinCoreEnvironment) {
    environment.updateClasspath(listOf(JvmClasspathRoot(KotlinTestUtils.findAndroidApiJar())))
}