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
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@DisableCachingByDefault
abstract class UnzipJsc : DefaultTask() {
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
    abstract val getIsLinux: Property<Boolean>

    @TaskAction
    fun extract() {
        fs.delete {
            delete(into)
        }

        fs.copy {
            from(
                archiveOperations.zipTree(from.singleFile)
            )
            into(into)
        }

        if (getIsLinux.get()) {
            val libDirectory = File(into.get().asFile, "lib")
            for (file in libDirectory.listFiles()) {
                if (file.isFile && file.length() < 100) { // seems unpacked file link
                    val linkTo = file.readText()
                    file.delete()
                    Files.createSymbolicLink(file.toPath(), File(linkTo).toPath())
                }
            }
        }
    }
}