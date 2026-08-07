@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.tooling.core.linearClosure

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

apply(from = "codegen.gradle.kts")

group = "org.jetbrains.kotlin"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions {
        optIn.add("org.jetbrains.kotlin.testFederation.DelicateTestFederationApi")

        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Test",
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

kotlin.sourceSets.main {
    generatedKotlin.srcDir(tasks.named("generateDomainSources"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    /* The shape of the entire repo is considered an input. Always re-run this task */
    doNotTrackState("The shape of the entire repo is considered as input (DomainsDump)")

    workingDir = gradle.linearClosure { it.parent }.last().rootProject.isolated.projectDirectory.asFile

    inputs.file(workingDir.resolve("repo/domains.yaml"))
        .withPathSensitivity(PathSensitivity.NONE)
        .withPropertyName("domains.yaml")

    environment("GRADLE_USER_HOME", gradle.gradleUserHomeDir.absolutePath)
}

dependencies {
    implementation(kotlin("tooling-core", version = embeddedKotlinVersion))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)
    compileOnly(libs.develocity.gradlePlugin)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.opentest4j)
    testImplementation(gradleTestKit())

    testImplementation(testFixtures(project(":repo-test-fixtures")))
}
