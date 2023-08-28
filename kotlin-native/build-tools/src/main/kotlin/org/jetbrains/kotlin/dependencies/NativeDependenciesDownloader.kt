/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.jetbrains.kotlin.konan.util.DependencyProcessor

/**
 * Downloader of native dependencies.
 *
 * Serves as a Gradle task wrapper around [DependencyProcessor].
 */
@UntrackedTask(because = "Output is large and work avoidance is performed in DependencyProcessor anyway")
abstract class NativeDependenciesDownloader : DefaultTask() {
    /**
     * [DependencyProcessor] that will perform downloading
     */
    @get:Internal
    abstract val dependencyProcessor: Property<DependencyProcessor>

    @TaskAction
    fun downloadAndExtract() {
        dependencyProcessor.get().run()
    }
}