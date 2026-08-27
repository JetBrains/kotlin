/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject

private abstract class MuteWithDatabaseArgumentProvider @Inject constructor(objects: ObjectFactory) : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val mutesFile: RegularFileProperty = objects.fileProperty()

    override fun asArguments(): Iterable<String> =
        listOf("-Dorg.jetbrains.kotlin.test.mutes.file=${mutesFile.get().asFile.canonicalPath}")
}

internal fun Project.configureTestMuting() {
    tasks.withType<Test>().configureEach {
        jvmArgumentProviders.add(
            project.objects.newInstance<MuteWithDatabaseArgumentProvider>().apply {
                mutesFile.fileValue(File(project.rootDir, "tests/mute-common.csv"))
            })
    }
}
