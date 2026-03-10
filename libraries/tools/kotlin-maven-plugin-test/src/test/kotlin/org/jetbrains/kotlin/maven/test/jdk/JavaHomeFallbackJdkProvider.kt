package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path

class JavaHomeFallbackJdkProvider(
    val mainProvider: JdkProvider
) : JdkProvider {
    override fun getJavaHome(version: TestVersions.Java): Path? {
        val jdk = mainProvider.getJavaHome(version)
        if (jdk == null) {
            println("Can't find JDK $version, falling back to Java Home. Tests may fail due to unexpected JDK version.")
            return Path(System.getProperty("java.home")!!)
        } else {
            return jdk
        }
    }
}