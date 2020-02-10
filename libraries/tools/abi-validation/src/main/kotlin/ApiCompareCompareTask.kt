/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation

import difflib.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import java.io.*

open class ApiCompareCompareTask : DefaultTask() {
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var projectApiDir: File

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var apiBuildDir: File

    @OutputFile
    @Optional
    val dummyOutputFile: File? = null

    @TaskAction
    fun verify() {
        if (!projectApiDir.exists())
            error("Expected folder with API declarations '$projectApiDir' does not exist")
        if (!apiBuildDir.exists())
            error("Folder with built API declarations '$apiBuildDir' does not exist")

        val subject = project.name
        val apiBuildDirFiles = mutableSetOf<RelativePath>()
        val expectedApiFiles = mutableSetOf<RelativePath>()
        project.fileTree(apiBuildDir).visit { file ->
            apiBuildDirFiles.add(file.relativePath)
        }
        project.fileTree(projectApiDir).visit { file ->
            expectedApiFiles.add(file.relativePath)
        }

        if (apiBuildDirFiles.size != 1) {
            error("Expected a single file $subject.api, but found: $expectedApiFiles")
        }

        val expectedApiDeclaration = apiBuildDirFiles.single()
        if (expectedApiDeclaration !in expectedApiFiles) {
            error("File ${expectedApiDeclaration.lastName} is missing from ${projectApiDir.relativePath()}, please run " +
                    ":$subject:apiDump task to generate one")
        }

        val diffSet = mutableSetOf<String>()
        val expectedFile = expectedApiDeclaration.getFile(projectApiDir)
        val actualFile = expectedApiDeclaration.getFile(apiBuildDir)
        val diff = compareFiles(expectedFile, actualFile)
        if (diff != null)
            diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            error("API check failed for project $subject.\n$diffText\n\n You can run :$subject:apiDump task to overwrite API declarations")
        }
    }

    private fun File.relativePath(): String {
        return relativeTo(project.rootProject.rootDir).toString() + "/"
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
