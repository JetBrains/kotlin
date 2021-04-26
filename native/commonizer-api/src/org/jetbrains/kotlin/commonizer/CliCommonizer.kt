/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import java.io.File
import java.net.URLClassLoader

public fun CliCommonizer(classpath: Iterable<File>): CliCommonizer {
    return CliCommonizer(URLClassLoader(classpath.map { it.absoluteFile.toURI().toURL() }.toTypedArray()))
}

public fun CliCommonizer(classLoader: ClassLoader): CliCommonizer {
    return CliCommonizer(CommonizerClassLoaderExecutor(classLoader))
}

public class CliCommonizer(private val executor: Executor) : Commonizer {

    public fun interface Executor {
        public operator fun invoke(arguments: List<String>)
    }

    override fun commonizeLibraries(
        konanHome: File,
        inputLibraries: Set<File>,
        dependencyLibraries: Set<CommonizerDependency>,
        outputCommonizerTarget: SharedCommonizerTarget,
        outputDirectory: File
    ) {
        if (inputLibraries.isEmpty()) return
        val arguments = mutableListOf<String>().apply {
            add("native-klib-commonize")
            add("-distribution-path"); add(konanHome.absolutePath)
            add("-input-libraries"); add(inputLibraries.joinToString(";") { it.absolutePath })
            add("-output-commonizer-target"); add(outputCommonizerTarget.identityString)
            add("-output-path"); add(outputDirectory.absolutePath)
            if (dependencyLibraries.isNotEmpty()) {
                add("-dependency-libraries"); add(dependencyLibraries.joinToString(";"))
            }
        }
        executor(arguments)
    }
}

private class CommonizerClassLoaderExecutor(private val commonizerClassLoader: ClassLoader) : CliCommonizer.Executor {
    companion object {
        private const val commonizerMainClass = "org.jetbrains.kotlin.commonizer.cli.CommonizerCLI"
        private const val commonizerMainFunction = "main"
    }

    @Throws(Throwable::class)
    override fun invoke(arguments: List<String>) {
        val commonizerMainClass = commonizerClassLoader.loadClass(commonizerMainClass)
        val commonizerMainMethod = commonizerMainClass.methods.singleOrNull { it.name == commonizerMainFunction }
            ?: throw IllegalArgumentException(
                "Missing or conflicting $commonizerMainFunction function in " +
                        "Class ${commonizerMainClass.name} from ClassLoader $commonizerClassLoader"
            )
        commonizerMainMethod.invoke(null, arguments.toTypedArray())
    }
}
