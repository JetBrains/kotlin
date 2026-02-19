/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

abstract class SystemPropertyClasspathProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${classpath.asPath}"
        )
    }
}

fun Test.addClasspathProperty(configuration: Configuration, property: String) {
    val classpathProvider = project.objects.newInstance(SystemPropertyClasspathProvider::class.java)
    classpathProvider.classpath.from(configuration)
    classpathProvider.property.set(property)
    jvmArgumentProviders.add(classpathProvider)
}
