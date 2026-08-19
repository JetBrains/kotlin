import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

description = "Binary Compatibility Validator compat - track ABI changes"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions.allWarningsAsErrors = true
}

dependencies {
    implementation(libs.javaDiffUtils)
    compileOnly(libs.kotlinx.bcv)
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
}

val generateBcvProperties = tasks.register("generateBcvProperties") {
    val generatedSrcDir = layout.buildDirectory.dir("src/generated/kotlin")
    outputs.dir(generatedSrcDir).withPropertyName("generatedSrcDir")
    outputs.cacheIf { true }

    val bcvVersion = libs.versions.kotlinx.bcv
    inputs.property("bcvVersion", bcvVersion)

    doLast {
        val outputDir = generatedSrcDir.get().asFile
        outputDir.mkdirs()
        outputDir.resolve("BcvProperties.kt").writeText(
            """
            |package org.jetbrains.kotlin.build.bcv.internal
            |
            |internal object BcvProperties {
            |    const val KOTLINX_BCV_VERSION = "${bcvVersion.get()}"
            |}
            |
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateBcvProperties)
}

gradlePlugin {
    plugins {
        create("bcvCompat") {
            id = "kotlin-git.gradle-build-conventions.binary-compatibility-extended"
            implementationClass = "org.jetbrains.kotlin.build.bcv.BcvCompatPlugin"
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
