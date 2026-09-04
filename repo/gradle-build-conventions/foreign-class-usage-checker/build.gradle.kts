import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

description = "Foreign Class Usage Checker – track dependency usage in libraries"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.intellij.asm)
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:$embeddedKotlinVersion")
    implementation(libs.diff.utils)
}

gradlePlugin {
    plugins {
        create("foreign-class-usage-checker") {
            id = "kotlin-git.gradle-build-conventions.foreign-class-usage-checker"
            implementationClass = "org.jetbrains.kotlin.build.foreign.ForeignClassUsageCheckerPlugin"
        }
    }
}

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
    "compilePluginsBlocksPluginClasspathElements",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
