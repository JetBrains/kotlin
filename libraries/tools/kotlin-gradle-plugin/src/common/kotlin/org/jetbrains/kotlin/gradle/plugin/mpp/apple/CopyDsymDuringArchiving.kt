/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only does copying")
internal abstract class CopyDsymDuringArchiving @Inject constructor(
    private val fsOperations: FileSystemOperations,
) : DefaultTask() {

    @get:Internal
    abstract val dsymPath: Property<File>

    @get:Optional
    @get:Input
    abstract val dwarfDsymFolderPath: Property<String>

    @TaskAction
    fun copy() {
        val dwarfDsymFolderPath = this.dwarfDsymFolderPath.orNull ?: return
        if (dwarfDsymFolderPath.isEmpty()) return
        fsOperations.copy {
            it.from(dsymPath.get()) { innerSpec ->
                innerSpec.into(dsymPath.get().name)
            }
            it.into(dwarfDsymFolderPath)
        }
    }

}