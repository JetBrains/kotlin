package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import java.nio.file.Path

interface JdkProvider {
    fun getJavaHome(version: TestVersions.Java): Path?
}

object CompositeJdkProvider : JdkProvider {
    private val providers = listOf<JdkProvider>(
        SystemPropertiesJdkProvider,
        EnvironmentJdkProvider,
        SdkmanJdkProvider,
        OsxJavaHomeProvider,
        LinuxJdkProvider,
        WindowsJdkProvider,
    )

    override fun getJavaHome(version: TestVersions.Java): Path? {
        return providers.firstNotNullOfOrNull {
            val javaHome = it.getJavaHome(version)
            if (javaHome != null) {
                println("Using JDK $javaHome from ${it.javaClass.name}")
                javaHome
            } else null
        }
    }
}
