/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

open class ConsistencyCheckArgumentsProvider @Inject constructor(
        objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {
    @get:Input
    val usePrebuiltSources = objectFactory.property(Boolean::class.java)

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val bindingsRoot = objectFactory.directoryProperty()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val generatedRoot = objectFactory.directoryProperty()

    @get:Input
    val projectName = objectFactory.property(String::class.java)

    @get:Input
    val regenerateTaskName = objectFactory.property(String::class.java)

    @get:Input
    val hostName = objectFactory.property(String::class.java)

    override fun asArguments() = listOf(
            "-DusePrebuiltSources=${usePrebuiltSources.get()}",
            "-DbindingsRoot=${bindingsRoot.get().asFile.absolutePath}",
            "-DgeneratedRoot=${generatedRoot.get().asFile.absolutePath}",
            "-DprojectName=${projectName.get()}",
            "-DregenerateTaskName=${regenerateTaskName.get()}",
            "-DhostName=${hostName.get()}",
    )
}