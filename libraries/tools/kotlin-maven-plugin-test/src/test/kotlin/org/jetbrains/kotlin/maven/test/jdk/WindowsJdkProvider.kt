package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object WindowsJdkProvider : JdkProvider {
    private val hotspotMsiRoots = listOf(
        "SOFTWARE\\AdoptOpenJDK\\JDK",
        "SOFTWARE\\Eclipse Adoptium\\JDK",
        "SOFTWARE\\Eclipse Foundation\\JDK"
    )

    private val javaHomeRoots = listOf(
        "SOFTWARE\\JavaSoft\\JDK",
        "SOFTWARE\\JavaSoft\\Java Development Kit",
        "SOFTWARE\\JavaSoft\\Java Runtime Environment",
        "SOFTWARE\\Wow6432Node\\JavaSoft\\Java Development Kit",
        "SOFTWARE\\Wow6432Node\\JavaSoft\\Java Runtime Environment"
    )

    private val jdkVersionRegex = """^(\d+)\.\d+.*$""".toRegex()

    private var discovered: Map<TestVersions.Java, Path>? = null

    private fun discover(): Map<TestVersions.Java, Path>? {
        val osName = System.getProperty("os.name")
        if (osName?.contains("Windows", ignoreCase = true) != true) {
            return null
        }

        val discovered = mutableMapOf<TestVersions.Java, Path>()

        hotspotMsiRoots.forEach { rootKey ->
            querySubkeys(rootKey).forEach { versionKey ->
                val javaVersion = parseJavaVersion(versionKey) ?: return@forEach
                val home = queryStringValue("$rootKey\\$versionKey\\hotspot\\MSI", "Path") ?: return@forEach
                val javaHome = Path(home)
                if (!javaHome.exists() || !javaHome.isDirectory()) return@forEach
                discovered.putIfAbsent(javaVersion, javaHome)
            }
        }

        javaHomeRoots.forEach { rootKey ->
            querySubkeys(rootKey).forEach { versionKey ->
                val javaVersion = parseJavaVersion(versionKey) ?: return@forEach
                val home = queryStringValue("$rootKey\\$versionKey", "JavaHome") ?: return@forEach
                val javaHome = Path(home)
                if (!javaHome.exists() || !javaHome.isDirectory()) return@forEach
                discovered.putIfAbsent(javaVersion, javaHome)
            }
        }

        return discovered
    }

    private fun parseJavaVersion(registryVersion: String): TestVersions.Java? {
        val matchResult = jdkVersionRegex.matchEntire(registryVersion) ?: return null
        val numericVersion = matchResult.groupValues[1].toIntOrNull() ?: return null
        return TestVersions.javaMapByNumericVersion[numericVersion]
    }

    private fun querySubkeys(key: String): List<String> {
        val output = runRegQuery(listOf("HKEY_LOCAL_MACHINE\\$key")) ?: return emptyList()
        val basePath = "HKEY_LOCAL_MACHINE\\$key"
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("HKEY_", ignoreCase = true) }
            .filter { it != basePath }
            .mapNotNull { fullPath ->
                fullPath.removePrefix("$basePath\\").takeIf { it != fullPath }
            }
            .toList()
    }

    private fun queryStringValue(key: String, valueName: String): String? {
        val output = runRegQuery(listOf("HKEY_LOCAL_MACHINE\\$key", "/v", valueName)) ?: return null
        val valueRegex = """^\s*${Regex.escape(valueName)}\s+REG_\w+\s+(.+)$""".toRegex()
        return output.lineSequence()
            .mapNotNull { line -> valueRegex.matchEntire(line)?.groupValues?.getOrNull(1) }
            .firstOrNull()
            ?.trim()
    }

    private fun runRegQuery(args: List<String>): String? {
        val process = runCatching {
            ProcessBuilder(listOf("reg", "query") + args)
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return null

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return output.takeIf { exitCode == 0 }
    }

    override fun getJavaHome(version: TestVersions.Java): Path? {
        if (discovered == null) {
            discovered = discover()
        }
        return discovered?.get(version)
    }
}
