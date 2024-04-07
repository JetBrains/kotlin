/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.process.CommandLineArgumentProvider

abstract class TestCompilerRuntimeArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val stdlibRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibMinimalRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinReflectJarForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibCommonRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val scriptRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinTestJarForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinAnnotationsForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val scriptingPluginForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibJsRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val testJsRuntimeForTests: ConfigurableFileCollection

    private fun ifNotEmpty(property: String, fileCollection: ConfigurableFileCollection): String? =
        if (fileCollection.isEmpty) null else argument(property, fileCollection)

    private fun argument(property: String, fileCollection: ConfigurableFileCollection) =
        "-D$property=${fileCollection.joinToString(",") { it.absolutePath }}"

    override fun asArguments(): Iterable<String> {
        println(
            listOf(
                "CRISTIAN",
                argument("kotlin.full.stdlib.path", stdlibRuntimeForTests),
                argument("kotlin.minimal.stdlib.path", stdlibMinimalRuntimeForTests),
                argument("kotlin.reflect.jar.path", kotlinReflectJarForTests),
                ifNotEmpty("kotlin.common.stdlib.path", stdlibCommonRuntimeForTests),
                ifNotEmpty("kotlin.script.runtime.path", scriptRuntimeForTests),
                ifNotEmpty("kotlin.test.jar.path", kotlinTestJarForTests),
                ifNotEmpty("kotlin.annotations.path", kotlinAnnotationsForTests),
                ifNotEmpty("kotlin.scriptingPlugin.classpath", scriptingPluginForTests),
            )
        )

        return listOfNotNull(
            argument("kotlin.full.stdlib.path", stdlibRuntimeForTests),
            argument("kotlin.minimal.stdlib.path", stdlibMinimalRuntimeForTests),
            argument("kotlin.reflect.jar.path", kotlinReflectJarForTests),
            ifNotEmpty("kotlin.common.stdlib.path", stdlibCommonRuntimeForTests),
            ifNotEmpty("kotlin.script.runtime.path", scriptRuntimeForTests),
            ifNotEmpty("kotlin.test.jar.path", kotlinTestJarForTests),
            ifNotEmpty("kotlin.annotations.path", kotlinAnnotationsForTests),
            ifNotEmpty("kotlin.scriptingPlugin.classpath", scriptingPluginForTests),
            ifNotEmpty("kotlin.js.stdlib.klib.path", stdlibJsRuntimeForTests),
            ifNotEmpty("kotlin.js.reduced.stdlib.path", stdlibJsRuntimeForTests),
            ifNotEmpty("kotlin.js.kotlin.test.klib.path", testJsRuntimeForTests),
        )
    }
}