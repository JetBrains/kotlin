/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.tukaani.xz.XZInputStream
import javax.inject.Inject

@DisableCachingByDefault
abstract class UnzipWasmtime : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputFiles
    @get:Classpath
    abstract val from: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val into: DirectoryProperty

    @get:Input
    abstract val getIsWindows: Property<Boolean>

    @TaskAction
    fun extract() {
        fs.delete {
            delete(into)
        }

        val inputFile = from.singleFile

        val isWindows = getIsWindows.get()

        // Repack .tar.xz archives into .tar due to an issue in the Gradle
        // https://github.com/gradle/gradle/issues/31858
        val archive = if (!isWindows) {
            val tarFile = temporaryDir.resolve(inputFile.nameWithoutExtension)
            XZInputStream(inputFile.inputStream().buffered()).use { xzIn ->
                tarFile.outputStream().buffered().use { tarOut ->
                    xzIn.copyTo(tarOut)
                }
            }
            tarFile
        } else {
            inputFile
        }

        fs.copy {
            from(
                if (isWindows) {
                    archiveOperations.zipTree(archive)
                } else {
                    archiveOperations.tarTree(archive)
                }
            )

            into(into)
        }
    }
}