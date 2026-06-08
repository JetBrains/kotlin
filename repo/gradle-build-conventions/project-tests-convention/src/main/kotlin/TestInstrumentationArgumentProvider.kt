/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.process.CommandLineArgumentProvider

abstract class TestInstrumentationArgumentProvider : CommandLineArgumentProvider {

    @get:InputFiles
    @get:Classpath
    abstract val agentJar: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val bootClasspathJar: ConfigurableFileCollection

    @get:Input
    abstract val debug: Property<Boolean>

    override fun asArguments(): Iterable<String> = buildList {
        add("-javaagent:${agentJar.singleFile}")
        add("-Xbootclasspath/a:${bootClasspathJar.singleFile}")
        if (debug.get()) {
            add("-Xlog:exceptions=info")
        }
    }
}
