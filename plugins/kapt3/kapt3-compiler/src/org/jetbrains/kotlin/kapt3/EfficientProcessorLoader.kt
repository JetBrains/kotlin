/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3

import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt3.base.ProcessorLoader
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.util.ServiceLoaderLite
import java.io.File
import java.net.URLClassLoader
import javax.annotation.processing.Processor

class EfficientProcessorLoader(options: KaptOptions, logger: KaptLogger) : ProcessorLoader(options, logger) {
    override fun doLoadProcessors(classpath: LinkedHashSet<File>, classLoader: ClassLoader): List<Processor> =
        when (classLoader) {
            is URLClassLoader -> ServiceLoaderLite.loadImplementations(Processor::class.java, classLoader)
            else -> super.doLoadProcessors(classpath, classLoader)
        }
}