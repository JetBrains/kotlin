package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object SystemPropertiesJdkProvider : JdkProvider {
    override fun getJavaHome(version: TestVersions.Java): Path? {
        val propertyName = "jdk${version.numericVersion}"
        val propertyValue = System.getProperty(propertyName) ?: return null
        val javaHome = Path(propertyValue)
        return javaHome.takeIf { it.exists() && it.isDirectory() }
    }
}
