import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // 'TestBuildCacheTeamCityCompatibilityFunctionalTest' drives a build of the whole repository, so
    // the repository is its input - all of it, which cannot be declared.
    doNotTrackState("Runs a build of the repository, whose state is not declared as an input")

    // The functional tests run Gradle against the repository itself, so they have to be started from
    // its build - './gradlew :gradle-build-conventions:project-tests-convention:test' - rather than
    // from a standalone build of these conventions, which knows no repository to run.
    val repositoryBuild = generateSequence(gradle.parent) { it.parent }.lastOrNull()
        ?: error("Run this task from the repository build: ':gradle-build-conventions:${project.name}:test'")
    // 'isolated' is incubating, and is still the accessor to use: it is the one that keeps working
    // when project isolation does. 'test-federation-convention' reads the repository root the same way.
    @Suppress("UnstableApiUsage")
    val repositoryRoot = repositoryBuild.rootProject.isolated.projectDirectory.asFile
    workingDir = repositoryRoot
    environment("GRADLE_USER_HOME", gradle.gradleUserHomeDir.absolutePath)
}

dependencies {
    api(project(":utilities"))

    implementation(libs.develocity.gradlePlugin)
    implementation(kotlinBuildHelpers())
    implementation(project(":d8-configuration"))

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    compileOnly(libs.node.gradlePlugin)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test-junit5"))
    testImplementation(gradleTestKit())

    constraints {
        api(libs.apache.commons.lang)
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
