package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object EnvironmentJdkProvider : JdkProvider {
    override fun getJavaHome(version: TestVersions.Java): Path? {
        val jdkVersion = when (version) {
            TestVersions.Java.JDK_1_8 -> "1_8"
            else -> version.numericVersion.toString()
        }

        val envVar1 = "JDK_${jdkVersion}"
        val envVar2 = "JDK_${jdkVersion}_0"
        val envValue = System.getenv(envVar1)
            ?: System.getenv(envVar2)
            ?: return null
        val javaHome = Path(envValue)
        return javaHome.takeIf { it.exists() && it.isDirectory() }
    }
}
