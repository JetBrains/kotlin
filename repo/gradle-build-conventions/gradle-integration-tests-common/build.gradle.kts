import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    @Suppress("DEPRECATION")
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    sourceSets.main.configure {
        kotlin.srcDir(
            layout.projectDirectory.dir("../../../libraries/tools/kotlin-gradle-plugin-integration-tests/src/integrationTestingVersions")
        )
    }
}
