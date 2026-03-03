/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

private const val DATAFRAME_CLASSPATH_KEY = "kotlin.dataframe.plugin.test.classpath"
internal val DATAFRAME_CLASSPATH = System.getProperty(DATAFRAME_CLASSPATH_KEY)
    ?.split(File.pathSeparator)?.map(::File)
    ?: error("Unable to get a valid classpath from '$DATAFRAME_CLASSPATH_KEY' property")

class DataFrameRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return DATAFRAME_CLASSPATH
    }
}