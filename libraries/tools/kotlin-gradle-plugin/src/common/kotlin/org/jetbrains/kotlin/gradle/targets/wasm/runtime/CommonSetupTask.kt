/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

@DisableCachingByDefault
abstract class CommonSetupTask @Inject constructor(
    settings: CommonEnvSpec,
) : AbstractSetupTask<CommonEnv, CommonEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "/"

    @get:Internal
    override val artifactModule: String = settings.moduleGroup

    @get:Internal
    override val artifactName: String = settings.name

    @get:Internal
    abstract val extractionAction: Property<(String, String, String, Path?) -> Unit>

    @get:Internal
    abstract val os: Property<String>

    @get:Internal
    abstract val arch: Property<String>

    @get:Internal
    abstract val version: Property<String>

    @get:Internal
    abstract val archiveOperation: Property<ArchiveOperationsProvider>

    override fun extract(archive: File) {
        val archiveOperationValue: ArchiveOperationsProvider = archiveOperation.getOrElse { ao: ArchiveOperations, path: Path ->
            when {
                path.fileName.toString().endsWith(".tar.gz") -> ao.tarTree(path)
                path.fileName.toString().endsWith(".zip") -> ao.zipTree(path)
                else -> error("Can't detect archive type for $path. Set archiveOperation.")
            }
        }

        fs.copy {
            it.from(
                archiveOperationValue.provideOperation(archiveOperations, archive.toPath())
            )
            it.into(destinationProvider)
        }

        extractionAction.orNull?.invoke(os.get(), arch.get(), version.get(), destinationProvider.getFile().toPath())
    }
}
