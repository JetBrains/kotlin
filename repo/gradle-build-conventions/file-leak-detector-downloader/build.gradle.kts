import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

description = "Download file-leak-detector JAR."

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)
    compilerOptions.allWarningsAsErrors = true
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        register("FileLeakDetectorDownloader") {
            id = "kotlin-git.gradle-build-conventions.file-leak-detector-downloader"
            implementationClass = "org.jetbrains.kotlin.build.FileLeakDetectorDownloaderPlugin"
        }
    }
}

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
