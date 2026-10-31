/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import javax.inject.Inject

/**
 * We symbolically link to the framework in BUILT_PRODUCTS_DIR for two purposes:
 * 1. Allow consuming the framework without specifying FRAMEWORK_SEARCH_PATHS, since Xcode uses BUILT_PRODUCTS_DIR as an implicit search path
 * 2. Enable using embedAndSign task in Pre-action scripts to compile Swift Package Manager target against Kotlin framework
 *
 * Xcode indexes with the Debug configuration whatever is being built, so the framework is also linked from
 * [XcodeEnvironment.indexBuiltProductsDirs]. Only the framework: the indexer needs headers and the module map, not the dSYM.
 */
@DisableCachingByDefault(because = "Build cache doesn't work with symbolic links")
internal abstract class SymbolicLinkToFrameworkTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Internal
    abstract val builtProductsDirectory: Property<File>

    @get:Internal
    abstract val indexBuiltProductsDirectories: ListProperty<File>

    @get:Internal
    abstract val frameworkPath: Property<File>

    // Symlink dSYM to make sure lldb works with dynamic framework symlink
    @get:Internal
    abstract val dsymPath: Property<File>

    @get:Input
    abstract val shouldDsymLinkExist: Property<Boolean>

    @get:Internal
    val frameworkSymbolicLinkPath: File get() = builtProductsDirectory.get().resolve(frameworkPath.get().name)

    @get:Internal
    val dsymSymbolicLinkPath: File get() = builtProductsDirectory.get().resolve(dsymPath.get().name)

    private val indexLinkCandidates: List<File>
        get() = indexBuiltProductsDirectories.get()
            .filter(::isIndexDirectoryExpected)
            .map { it.resolve(frameworkPath.get().name) }

    @get:Internal
    val indexSymbolicLinkPaths: List<File>
        get() = indexLinkCandidates.filterNot(::isForeignEntry)

    init {
        outputs.upToDateWhen {
            val frameworkLinkUpToDate = isSymbolicLinkUpToDate(
                linkPath = frameworkSymbolicLinkPath.toPath(),
                destinationPath = frameworkPath.get().toPath()
            )
            val indexLinksUpToDate = indexSymbolicLinkPaths.all { linkPath ->
                isSymbolicLinkUpToDate(
                    linkPath = linkPath.toPath(),
                    destinationPath = frameworkPath.get().toPath()
                )
            }
            if (shouldDsymLinkExist.get()) {
                frameworkLinkUpToDate && indexLinksUpToDate && isSymbolicLinkUpToDate(
                    linkPath = dsymSymbolicLinkPath.toPath(),
                    destinationPath = dsymPath.get().toPath()
                )
            } else {
                frameworkLinkUpToDate && indexLinksUpToDate && !dsymSymbolicLinkPath.exists()
            }
        }
    }

    // Xcode creates the Index.noindex arena when the project is opened in the editor. Don't invent it: only link into
    // directories whose parent exists.
    private fun isIndexDirectoryExpected(directory: File): Boolean = directory.parentFile?.exists() == true

    // A real directory there is someone else's product (an older KGP copied frameworks, CocoaPods, the user). Leave it alone;
    // the KT-68257 clean-up below is only meant for BUILT_PRODUCTS_DIR of the configuration being built.
    private fun isForeignEntry(linkPath: File): Boolean {
        val path = linkPath.toPath()
        return !Files.isSymbolicLink(path) && Files.exists(path, LinkOption.NOFOLLOW_LINKS)
    }

    private fun isSymbolicLinkUpToDate(
        linkPath: Path,
        destinationPath: Path
    ): Boolean = Files.isSymbolicLink(linkPath) && Files.readSymbolicLink(linkPath) == destinationPath

    @TaskAction
    fun writeSymbolicLinks() {
        updateSymbolicLink(
            linkPath = frameworkSymbolicLinkPath.toPath(),
            destinationPath = frameworkPath.get().toPath(),
            shouldLinkExist = true,
        )
        updateSymbolicLink(
            linkPath = dsymSymbolicLinkPath.toPath(),
            destinationPath = dsymPath.get().toPath(),
            shouldLinkExist = shouldDsymLinkExist.get(),
        )
        for (linkPath in indexLinkCandidates) {
            if (isForeignEntry(linkPath)) {
                logger.warn("Not linking the framework for Xcode indexing: $linkPath exists and is not a symbolic link")
                continue
            }
            createXcodeBuildSystemDirectory(linkPath.parentFile, execOperations)
            updateSymbolicLink(
                linkPath = linkPath.toPath(),
                destinationPath = frameworkPath.get().toPath(),
                shouldLinkExist = true,
            )
        }
    }

    private fun updateSymbolicLink(
        linkPath: Path,
        destinationPath: Path,
        shouldLinkExist: Boolean
    ) {
        if (Files.isSymbolicLink(linkPath)) {
            Files.deleteIfExists(linkPath)
        } else if (Files.exists(linkPath)) {
            // KT-68257: in 2.0.0 we create directory in BUILT_PRODUCTS_DIR, so it must be removed before symbolic link can be installed
            linkPath.toFile().deleteRecursively()
        }

        if (shouldLinkExist) {
            Files.createSymbolicLink(
                linkPath,
                destinationPath,
            )
        }
    }

}
