/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.nio.file.Files
import javax.inject.Inject

abstract class UnzipWasmEdge : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:InputFiles
    @get:Classpath
    abstract val from: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val into: DirectoryProperty

    @get:Input
    abstract val getIsWindows: Property<Boolean>

    @get:Input
    abstract val getIsMac: Property<Boolean>

    @get:Input
    abstract val directoryName: Property<String>

    @TaskAction
    fun extract() {
        fs.delete {
            delete(into)
        }

        fs.copy {
            from(from)
            into(into)
        }

        if (getIsWindows.get()) return

        val wasmEdgeUnpackedDirectory = into.get().asFile.resolve(directoryName.get())

        val unpackedWasmEdgeDirectory = wasmEdgeUnpackedDirectory.toPath()

        val libDirectory = unpackedWasmEdgeDirectory
            .resolve(if (getIsMac.get()) "lib" else "lib64")

        val targets = if (getIsMac.get())
            listOf("libwasmedge.0.1.0.dylib", "libwasmedge.0.1.0.tbd")
        else listOf("libwasmedge.so.0.1.0")

        targets.forEach {
            val target = libDirectory.resolve(it)
            val firstLink = libDirectory.resolve(it.replace("0.1.0", "0")).also(Files::deleteIfExists)
            val secondLink = libDirectory.resolve(it.replace(".0.1.0", "")).also(Files::deleteIfExists)

            Files.createSymbolicLink(firstLink, target)
            Files.createSymbolicLink(secondLink, target)
        }
    }
}