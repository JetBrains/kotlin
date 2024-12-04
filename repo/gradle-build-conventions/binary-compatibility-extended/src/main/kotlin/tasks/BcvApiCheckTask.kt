/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.tasks

import org.jetbrains.kotlin.build.bcv.BcvCompatPlugin.Companion.API_DUMP_TASK_NAME
import org.jetbrains.kotlin.build.bcv.internal.GradlePath
import org.jetbrains.kotlin.build.bcv.internal.fullPath
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.File
import java.util.*
import javax.inject.Inject

@CacheableTask
abstract class BcvApiCheckTask
@Inject
constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(RELATIVE)
    val projectApiDir: Provider<File>
        // workaround for https://github.com/gradle/gradle/issues/2016
        get() = expectedApiDirPath.flatMap { providers.provider { File(it).takeIf(File::exists) } }

    @get:Input
    @get:Optional
    abstract val expectedApiDirPath: Property<String>

    @get:InputDirectory
    @get:PathSensitive(RELATIVE)
    abstract val apiBuildDir: DirectoryProperty

    @get:Input
    internal abstract val expectedProjectName: Property<String>

    // Project and tasks paths are used for creating better error messages
    private val projectFullPath = project.fullPath
    private val apiDumpTaskPath = GradlePath(project.path).child(API_DUMP_TASK_NAME)

    private val rootDir = project.rootProject.rootDir

    @TaskAction
    fun verify() {
        val projectApiDir = projectApiDir.orNull
            ?: error(
                """
                Expected folder with API declarations '${expectedApiDirPath.get()}' does not exist.
                Please ensure that task '$apiDumpTaskPath' was executed in order to get API dump to compare the build against
                """.trimIndent()
            )

        val apiBuildDir = apiBuildDir.get().asFile

        val checkApiDeclarationPaths = projectApiDir.relativePathsOfContent { !isDirectory }
        val builtApiDeclarationPaths = apiBuildDir.relativePathsOfContent { !isDirectory }
        logger.info("checkApiDeclarationPaths: $checkApiDeclarationPaths")

        checkApiDeclarationPaths.forEach { checkApiDeclarationPath ->
            logger.info("---------------------------")
            checkTarget(
                checkApiDeclaration = checkApiDeclarationPath.getFile(projectApiDir),
                // fetch the builtFile, using the case-insensitive map
                builtApiDeclaration = builtApiDeclarationPaths[checkApiDeclarationPath]?.getFile(apiBuildDir)
            )
            logger.info("---------------------------")
        }
    }

    private fun checkTarget(
        checkApiDeclaration: File,
        builtApiDeclaration: File?,
    ) {
        logger.info("checkApiDeclaration: $checkApiDeclaration")
        logger.info("builtApiDeclaration: $builtApiDeclaration")

        val allBuiltFilePaths = builtApiDeclaration?.parentFile.relativePathsOfContent()
        val allCheckFilePaths = checkApiDeclaration.parentFile.relativePathsOfContent()

        logger.info("allBuiltPaths: $allBuiltFilePaths")
        logger.info("allCheckFiles: $allCheckFilePaths")

        val builtFilePath = allBuiltFilePaths.singleOrNull()
            ?: error("Expected a single file ${expectedProjectName.get()}.api, but found ${allBuiltFilePaths.size}: $allBuiltFilePaths")

        if (builtApiDeclaration == null || builtFilePath !in allCheckFilePaths) {
            val relativeDirPath = projectApiDir.get().toRelativeString(rootDir) + File.separator
            error(
                "File ${builtFilePath.lastName} is missing from ${relativeDirPath}, please run '$apiDumpTaskPath' task to generate one"
            )
        }

        val diffText = compareFiles(
            checkFile = checkApiDeclaration,
            builtFile = builtApiDeclaration,
        )?.trim()

        if (!diffText.isNullOrBlank()) {
            error(
                """
                |API check failed for project $projectFullPath.
                |
                |$diffText
                |
                |You can run '$apiDumpTaskPath' task to overwrite API declarations
                """.trimMargin()
            )
        }
    }

    /** Get the relative paths of all files and folders inside a directory */
    private fun File?.relativePathsOfContent(
        filter: FileVisitDetails.() -> Boolean = { true },
    ): RelativePaths {
        val contents = RelativePaths()
        if (this != null) {
            objects.fileTree().from(this).visit {
                if (filter()) contents += relativePath
            }
        }
        return contents
    }

    private fun compareFiles(checkFile: File, builtFile: File): String? {
        val checkText = checkFile.readText()
        val builtText = builtFile.readText()

        // We don't compare full text because newlines on Windows & Linux/macOS are different
        val checkLines = checkText.lines()
        val builtLines = builtText.lines()
        if (checkLines == builtLines)
            return null

        val patch = DiffUtils.diff(checkLines, builtLines)
        val diff = UnifiedDiffUtils.generateUnifiedDiff(
            checkFile.toString(),
            builtFile.toString(),
            checkLines,
            patch,
            3,
        )
        return diff.joinToString("\n")
    }
}

/*
 * We use case-insensitive comparison to workaround issues with case-insensitive OSes and Gradle
 * behaving slightly different on different platforms. We neither know original sensitivity of
 * existing .api files, not build ones, because projectName that is part of the path can have any
 * sensitivity. To work around that, we replace paths we are looking for the same paths that
 * actually exist on the FS.
 */
private class RelativePaths(
    private val map: TreeMap<RelativePath, RelativePath> = caseInsensitiveMap(),
) : Set<RelativePath> by map.keys {

    operator fun plusAssign(path: RelativePath) {
        map[path] = path
    }

    operator fun get(path: RelativePath): RelativePath? = map[path]

    companion object {
        private fun caseInsensitiveMap() =
            TreeMap<RelativePath, RelativePath> { path1, path2 ->
                path1.toString().compareTo(path2.toString(), true)
            }
    }

    override fun toString(): String =
        map.keys.joinToString(
            prefix = "RelativePaths(",
            separator = "/",
            postfix = ")",
            transform = RelativePath::getPathString,
        )
}
