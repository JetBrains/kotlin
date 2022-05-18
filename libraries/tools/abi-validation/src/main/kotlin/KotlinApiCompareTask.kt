/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import difflib.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import java.io.*
import java.util.TreeMap
import javax.inject.Inject

open class KotlinApiCompareTask @Inject constructor(private val objects: ObjectFactory): DefaultTask() {

    /*
     * Nullability and optionality is a workaround for
     * https://github.com/gradle/gradle/issues/2016
     *
     * Unfortunately, there is no way to skip validation apart from setting 'null'
     */
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    var projectApiDir: File? = null

    // Used for diagnostic error message when projectApiDir doesn't exist
    @Input
    @Optional
    var nonExistingProjectApiDir: String? = null

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var apiBuildDir: File

    @OutputFile
    @Optional
    @Suppress("unused")
    val dummyOutputFile: File? = null

    private val projectName = project.name

    private val rootDir = project.rootProject.rootDir

    @TaskAction
    fun verify() {
        val projectApiDir = projectApiDir
            ?: error("Expected folder with API declarations '$nonExistingProjectApiDir' does not exist.\n" +
                    "Please ensure that ':apiDump' was executed in order to get API dump to compare the build against")

        val subject = projectName

        /*
         * We use case-insensitive comparison to workaround issues with case-insensitive OSes
         * and Gradle behaving slightly different on different platforms.
         * We neither know original sensitivity of existing .api files, not
         * build ones, because projectName that is part of the path can have any sensitvity.
         * To workaround that, we replace paths we are looking for the same paths that
         * actually exist on FS.
         */
        fun caseInsensitiveMap() = TreeMap<RelativePath, RelativePath> { rp, rp2 ->
            rp.toString().compareTo(rp2.toString(), true)
        }

        val apiBuildDirFiles = caseInsensitiveMap()
        val expectedApiFiles = caseInsensitiveMap()

        objects.fileTree().from(apiBuildDir).visit { file ->
            apiBuildDirFiles[file.relativePath] = file.relativePath
        }
        objects.fileTree().from(projectApiDir).visit { file ->
            expectedApiFiles[file.relativePath] = file.relativePath
        }

        if (apiBuildDirFiles.size != 1) {
            error("Expected a single file $subject.api, but found: $expectedApiFiles")
        }

        var expectedApiDeclaration = apiBuildDirFiles.keys.single()
        if (expectedApiDeclaration !in expectedApiFiles) {
            error("File ${expectedApiDeclaration.lastName} is missing from ${projectApiDir.relativePath()}, please run " +
                    ":$subject:apiDump task to generate one")
        }
        // Normalize case-sensitivity
        expectedApiDeclaration = expectedApiFiles.getValue(expectedApiDeclaration)
        val actualApiDeclaration = apiBuildDirFiles.getValue(expectedApiDeclaration)
        val diffSet = mutableSetOf<String>()
        val expectedFile = expectedApiDeclaration.getFile(projectApiDir)
        val actualFile = actualApiDeclaration.getFile(apiBuildDir)
        val diff = compareFiles(expectedFile, actualFile)
        if (diff != null) diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            error("API check failed for project $subject.\n$diffText\n\n You can run :$subject:apiDump task to overwrite API declarations")
        }
    }

    private fun File.relativePath(): String {
        return relativeTo(rootDir).toString() + "/"
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
        val diff = DiffUtils.generateUnifiedDiff(checkFile.toString(), builtFile.toString(), checkLines, patch, 3)
        return diff.joinToString("\n")
    }
}
