/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import java.io.*
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.tasks.*

public open class KotlinApiCompareTask @Inject constructor(): DefaultTask() {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public lateinit var projectApiFile: File

    @InputFiles
    public lateinit var generatedApiFile: File

    private val projectName = project.name

    private val rootDir = project.rootProject.rootDir

    @TaskAction
    internal fun verify() {
        val projectApiDir = projectApiFile.parentFile
        if (!projectApiDir.exists()) {
            error("Expected folder with API declarations '$projectApiDir' does not exist.\n" +
                    "Please ensure that ':apiDump' was executed in order to get API dump to compare the build against")
        }
        val buildApiDir = generatedApiFile.parentFile
        if (!buildApiDir.exists()) {
            error("Expected folder with generate API declarations '$buildApiDir' does not exist.")
        }
        val subject = projectName

        if (!projectApiFile.exists()) {
            error("File ${projectApiFile.name} is missing from ${projectApiDir.relativeDirPath()}, please run " +
                    ":$subject:apiDump task to generate one")
        }
        if (!generatedApiFile.exists()) {
            error("File ${generatedApiFile.name} is missing from dump results.")
        }

        // Normalize case-sensitivity
        val diffSet = mutableSetOf<String>()
        val diff = compareFiles(projectApiFile, generatedApiFile)
        if (diff != null) diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            error("API check failed for project $subject.\n$diffText\n\n You can run :$subject:apiDump task to overwrite API declarations")
        }
    }

    private fun File.relativeDirPath(): String {
        return toRelativeString(rootDir) + File.separator
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
        val diff = UnifiedDiffUtils.generateUnifiedDiff(checkFile.toString(), builtFile.toString(), checkLines, patch, 3)
        return diff.joinToString("\n")
    }
}
