package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import java.nio.file.Paths

object SdkmanJdkProvider : JdkProvider {
    private val sdkmanCandidatesLocation =
        Paths.get(System.getProperty("user.home"), ".sdkman", "candidates", "java")

    private val sdkManCandidateRegex = """(\d+)\.\d+\.\d+-\w+""".toRegex()

    private var discovered: Map<TestVersions.Java, Path>? = null

    private fun discover(): Map<TestVersions.Java, Path>? {
        if (!sdkmanCandidatesLocation.toFile().exists()) {
            return null
        }
        val discovered = mutableMapOf<TestVersions.Java, Path>()
        sdkmanCandidatesLocation.toFile().listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            val matchResult = sdkManCandidateRegex.matchEntire(dir.name) ?: return@forEach
            val numericVersion = matchResult.groupValues[1].toIntOrNull() ?: return@forEach
            val javaVersion = TestVersions.javaMapByNumericVersion[numericVersion] ?: return@forEach

            discovered[javaVersion] = dir.toPath()
        }
        return discovered
    }

    override fun getJavaHome(version: TestVersions.Java): Path? {
        if (discovered == null) {
            discovered = discover()
        }
        return discovered?.get(version)
    }
}