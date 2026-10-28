import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    @Suppress("DEPRECATION")
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)
    compilerOptions.allWarningsAsErrors = true
}

// Sibling conventions such as test-inputs-check are built for the running JDK, not for 17
java {
    disableAutoTargetJvm()
}

dependencies {
    implementation(project(":buildsrc-compat"))
    implementation(project(":test-inputs-check"))
    // Only the extension type is needed here; at runtime common-configuration applies the convention itself
    compileOnly(project(":project-tests-convention"))
    implementation(kotlinBuildHelpers())
    implementation(libs.kotlinx.bcv)
    // Generated version catalog accessors, so the precompiled script can read `libs`
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
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
