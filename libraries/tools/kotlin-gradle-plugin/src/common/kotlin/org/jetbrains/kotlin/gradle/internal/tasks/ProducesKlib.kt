/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

internal interface ProducesKlib : Task {
    @get:Inject
    val projectLayout: ProjectLayout

    @get:Input
    val produceUnpackagedKlib: Property<Boolean>

    @get:Internal
    val klibOutput: Provider<File>

    @get:Internal
    val klibFile: Provider<RegularFile>
        get() = projectLayout.file(klibOutput).zip(produceUnpackagedKlib) { out, isUnpackaged ->
            require(!isUnpackaged) {
                error("The task $path is configured to produce unpackaged Klibs, but the consumer tries to use it as a regular file. Please file an issue at https://kotl.in/issue")
            }
            out
        }

    @get:Internal
    val klibDirectory: Provider<Directory>
        get() = projectLayout.dir(klibOutput).zip(produceUnpackagedKlib) { out, isUnpackaged ->
            require(isUnpackaged) {
                "The task $path is configured to produce packaged Klibs, but the consumer tries to use it as a directory. Please file an issue at https://kotl.in/issue"
            }
            out
        }
}