package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object OsxJavaHomeProvider : JdkProvider {
    // Example:
    //    17.0.9 (arm64) "JetBrains s.r.o." - "JBR-17.0.9+8-1166.2-nomod 17.0.9" /Users/user/Library/Java/JavaVirtualMachines/jbr-17.0.9/Contents/Home
    private val javaHomeLineRegex =
        """^\s*(\d+)\.\d+\.\d+ \(\w+\) ".+" - ".+" (/.*)$""".toRegex()

    private var discovered: Map<TestVersions.Java, Path>? = null

    private fun discover(): Map<TestVersions.Java, Path>? {
        val osName = System.getProperty("os.name")
        if (osName?.contains("Mac", ignoreCase = true) != true) {
            return null
        }

        val process = runCatching {
            ProcessBuilder("/usr/libexec/java_home", "-V")
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return null

        val discovered = mutableMapOf<TestVersions.Java, Path>()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val matchResult = javaHomeLineRegex.matchEntire(line) ?: return@forEach
                val numericVersion = matchResult.groupValues[1].toIntOrNull() ?: return@forEach
                val javaVersion = TestVersions.javaMapByNumericVersion[numericVersion] ?: return@forEach
                val javaHome= Path(matchResult.groupValues[2])
                if (!javaHome.exists() || !javaHome.isDirectory()) return@forEach

                discovered.putIfAbsent(javaVersion, javaHome)
            }
        }

        process.waitFor()
        return discovered
    }

    override fun getJavaHome(version: TestVersions.Java): Path? {
        if (discovered == null) {
            discovered = discover()
        }
        return discovered?.get(version)
    }
}