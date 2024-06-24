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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

public open class KotlinApiCompareTask : DefaultTask() {

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val projectApiFile: RegularFileProperty = project.objects.fileProperty()

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val generatedApiFile: RegularFileProperty = project.objects.fileProperty()

    private val projectName = project.name

    private val rootDir = project.rootDir

    @TaskAction
    internal fun verify() {
        val projectApiFile = projectApiFile.get().asFile
        val generatedApiFile = generatedApiFile.get().asFile

        if (!projectApiFile.exists()) {
            error(
                "Expected file with API declarations '${projectApiFile.relativeTo(rootDir)}' does not exist.\n" +
                        "Please ensure that ':apiDump' was executed in order to get " +
                        "an API dump to compare the build against"
            )
        }
        if (!generatedApiFile.exists()) {
            error(
                "Expected file with generated API declarations '${generatedApiFile.relativeTo(rootDir)}'" +
                        " does not exist."
            )
        }

        val diffSet = mutableSetOf<String>()
        val diff = compareFiles(projectApiFile, generatedApiFile)
        if (diff != null) diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            val subject = projectName
            error(
                "API check failed for project $subject.\n$diffText\n\n" +
                        "You can run :$subject:apiDump task to overwrite API declarations"
            )
        }
    }

    private fun compareFiles(checkFile: File, builtFile: File): String? {
        val checkText = checkFile.readText()
        val builtText = builtFile.readText()

        // We don't compare a full text because newlines on Windows & Linux/macOS are different
        val checkLines = checkText.lines()
        val builtLines = builtText.lines()
        if (checkLines == builtLines)
            return null

        val patch = DiffUtils.diff(checkLines, builtLines)
        val diff =
            UnifiedDiffUtils.generateUnifiedDiff(checkFile.toString(), builtFile.toString(), checkLines, patch, 3)
        return diff.joinToString("\n")
    }
}
