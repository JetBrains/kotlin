/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

internal interface ProducesKlib : Task {
    @get:Inject
    val projectLayout: ProjectLayout

    @get:Internal
    val klibOutput: Provider<File>

    @get:Internal
    val klibDirectory: Provider<Directory>
        get() = projectLayout.dir(klibOutput)
}
