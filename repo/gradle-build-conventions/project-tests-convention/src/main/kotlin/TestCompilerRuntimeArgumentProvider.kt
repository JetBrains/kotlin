/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import TestCompilePaths.KOTLIN_TESTDATA_ROOTS
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

abstract class TestCompilerRuntimeArgumentProvider : CommandLineArgumentProvider {
    /**
     * Testdata roots as `<path relative to the repository root> -> <absolute path>` pairs, passed to
     * the test JVM. Being an [Input] of plain strings, it is cheap to declare and stays the same for
     * every test task of a project, see [TestDataInputs.files].
     */
    @get:Input
    abstract val testDataMap: MapProperty<String, String>

    /**
     * Testdata snapshotted as an input of the task, which may be narrower than [testDataMap].
     * See [TestDataInputs.files].
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testDataFiles: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        return listOfNotNull(
            testDataMap.get().takeIf { it.isNotEmpty() }
                ?.map { it.key + "=" + it.value }
                ?.joinToString(prefix = "-D$KOTLIN_TESTDATA_ROOTS=", separator = ";"),
        )
    }
}
