package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

object LinuxJdkProvider : JdkProvider {
    private val jdkLocations = listOf(
        Path("/usr/lib/jvm"),
        Path("/usr/lib64/jvm"),
        Path("/usr/java"),
        Path("/usr/local/java"),
        Path("/opt/java")
    )

    private val jdkDirectoryRegex = """^(\d+)\.\d+.*$""".toRegex()

    private var discovered: Map<TestVersions.Java, Path>? = null

    private fun discover(): Map<TestVersions.Java, Path>? {
        val osName = System.getProperty("os.name")
        if (osName?.contains("Linux", ignoreCase = true) != true) {
            return null
        }

        val discovered = mutableMapOf<TestVersions.Java, Path>()
        jdkLocations.forEach { location ->
            if (!location.exists() || !location.isDirectory()) return@forEach

            location.listDirectoryEntries().forEach { dir ->
                if (!dir.isDirectory()) return@forEach
                val matchResult = jdkDirectoryRegex.matchEntire(dir.name) ?: return@forEach
                val numericVersion = matchResult.groupValues[1].toIntOrNull() ?: return@forEach
                val javaVersion = TestVersions.javaMapByNumericVersion[numericVersion] ?: return@forEach
                discovered.putIfAbsent(javaVersion, dir)
            }
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
