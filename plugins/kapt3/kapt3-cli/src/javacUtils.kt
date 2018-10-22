/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli

import java.io.File

internal const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

internal fun areJavacComponentsAvailable(): Boolean {
    return try {
        Class.forName(JAVAC_CONTEXT_CLASS)
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}

internal fun findToolsJar(): File? {
    val currentJavaHome = System.getProperty("java.home")
        ?.takeIf { it.isNotEmpty() }
        ?.let(::File)
        ?.takeIf { it.exists() }
        ?: return null

    fun getToolsJar(javaHome: File) = File(javaHome, "lib/tools.jar").takeIf { it.exists() }

    getToolsJar(currentJavaHome)?.let { return it}

    if (currentJavaHome.name == "jre") {
        getToolsJar(currentJavaHome.parentFile)?.let { return it}
    }

    return null
}