/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import java.io.File
import java.nio.file.Files.createTempDirectory

class GitHubOwnersTest : TestCase() {

    fun testGitHubCodeOwnersWasGeneratedCorrectly() {
        val scriptFilePath = System.getProperty("codeOwnersTest.scriptFile") ?: error("Missing property")
        val spaceCodeOwnersFilePath = System.getProperty("codeOwnersTest.spaceCodeOwnersFile") ?: error("Missing property")
        val githubCodeOwnersFilePath = System.getProperty("codeOwnersTest.githubCodeOwnersFile") ?: error("Missing property")

        val scriptFile = File(scriptFilePath)
        assertTrue("Script file does not exist: ${scriptFile.absolutePath}", scriptFile.exists())

        val tempDir = createTempDirectory("github-codeowners-test").toFile()
        tempDir.deleteOnExit()

        val tempSpaceDir = File(tempDir, ".space")
        tempSpaceDir.mkdirs()

        File(spaceCodeOwnersFilePath).copyTo(File(tempSpaceDir, "CODEOWNERS"))
        scriptFile.copyTo(File(tempSpaceDir, "generate-github-codeowners.sh"))

        val tempGithubDir = File(tempDir, ".github")
        tempGithubDir.mkdirs()

        try {
            val process = ProcessBuilder("bash", File(tempSpaceDir, "generate-github-codeowners.sh").absolutePath)
                .directory(tempDir)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            assertEquals("Script execution failed with exit code: $exitCode", 0, exitCode)

            val generatedFile = File(tempGithubDir, "CODEOWNERS")
            assertTrue("Script did not generate the CODEOWNERS file", generatedFile.exists())

            val originalFile = File(githubCodeOwnersFilePath)
            assertEquals(
                "Generated GitHub CODEOWNERS does not match the actual file, did you forget to run .space/generate-github-codeowners.sh?",
                generatedFile.readText(),
                originalFile.readText()
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
