/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.process.CommandLineArgumentProvider

abstract class TestInstrumentationArgumentProvider : CommandLineArgumentProvider {

    @get:InputFiles
    @get:Classpath
    abstract val agentJar: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val bootClasspathJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf(
            "-javaagent:${agentJar.singleFile}",
            "-Xbootclasspath/a:${bootClasspathJar.singleFile}"
        )
}
