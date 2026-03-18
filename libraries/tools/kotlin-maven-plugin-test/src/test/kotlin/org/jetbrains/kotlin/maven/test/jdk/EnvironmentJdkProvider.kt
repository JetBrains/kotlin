package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object EnvironmentJdkProvider : JdkProvider {
    override fun getJavaHome(version: TestVersions.Java): Path? {
        val envVar = "JDK_${version.numericVersion}"
        val envValue = System.getenv(envVar) ?: return null
        val javaHome = Path(envValue)
        return javaHome.takeIf { it.exists() && it.isDirectory() }
    }
}
