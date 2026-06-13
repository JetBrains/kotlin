/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import TestCompilePaths.KOTLIN_TESTDATA_ROOTS
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

abstract class TestCompilerRuntimeArgumentProvider : CommandLineArgumentProvider {
    @get:Input
    abstract val testDataMap: MapProperty<String, String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testDataFiles: ListProperty<Directory>

    override fun asArguments(): Iterable<String> {
        return listOfNotNull(
            testDataMap.get().takeIf { it.isNotEmpty() }
                ?.map { it.key + "=" + it.value }
                ?.joinToString(prefix = "-D$KOTLIN_TESTDATA_ROOTS=", separator = ";"),
        )
    }
}
