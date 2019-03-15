/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import java.io.File
import java.util.concurrent.Callable

class KotlinWithJavaCompilationOutput(
    internal val compilation: KotlinWithJavaCompilation<*>
) : KotlinCompilationOutput, Callable<FileCollection> {

    private val javaSourceSetOutput
        get() = compilation.javaSourceSet.output

    override val resourcesDir: File
        get() = javaSourceSetOutput.resourcesDir!!

    override var resourcesDirProvider: Any
        get() = javaSourceSetOutput.resourcesDir!!
        set(value) {
            javaSourceSetOutput.setResourcesDir(value)
        }

    override val classesDirs: ConfigurableFileCollection =
        javaSourceSetOutput.classesDirs as ConfigurableFileCollection

    override val allOutputs: FileCollection
        get() = javaSourceSetOutput

    override fun call(): FileCollection = allOutputs
}
