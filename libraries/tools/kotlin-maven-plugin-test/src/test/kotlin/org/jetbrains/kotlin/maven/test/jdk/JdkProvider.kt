package org.jetbrains.kotlin.maven.test.jdk

import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.isTeamCityRun
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

    private val cache = mutableMapOf<TestVersions.Java, Path>()

    override fun getJavaHome(version: TestVersions.Java): Path? {
        val cached = cache[version]
        if (cached != null) return cached

        return providers.firstNotNullOfOrNull {
            val javaHome = it.getJavaHome(version)
            if (javaHome != null) {
                println("Using '$javaHome' from ${it.javaClass.simpleName} for $version")
                javaHome
            } else null
        }.also { if (it != null) cache[version] = it }
    }
}
