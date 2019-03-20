/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

open class KotlinJsNodeModulesTask : DefaultTask() {
    @OutputDirectory
    @SkipWhenEmpty
    lateinit var nodeModulesDir: File

    @Classpath
    @InputFiles
    @SkipWhenEmpty
    lateinit var classpath: FileCollection

    @TaskAction
    fun copyFromRuntimeClasspath() {
        project.sync { sync ->
            sync.includeEmptyDirs = false

            classpath.forEach {
                if (it.isZip) sync.from(project.zipTree(it))
                else sync.from(it)
            }

            sync.include { fileTreeElement ->
                isKotlinJsRuntimeFile(fileTreeElement.file)
            }

            sync.into(nodeModulesDir)
        }
    }
}

private val File.isZip
    get() = isFile && name.endsWith(".jar")

internal fun isKotlinJsRuntimeFile(file: File): Boolean {
    if (!file.isFile) return false
    val name = file.name
    return (name.endsWith(".js") && !name.endsWith(".meta.js"))
            || name.endsWith(".js.map")
}