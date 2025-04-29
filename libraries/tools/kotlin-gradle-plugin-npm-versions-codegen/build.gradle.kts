import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.yarn.task.YarnTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(libs.ktor.client.cio)
    implementation(libs.gson)
    implementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation(libs.kotlinx.serialization.core)
}

node {
    download.set(true)
    version.set(nodejsVersion)
}

val npmProjectDefault = project.layout.buildDirectory.map { it.dir("npm-project") }
val kotlinGradlePluginProjectDir = project(":kotlin-gradle-plugin").projectDir
val kotlinGradlePluginIntegrationTestsProjectDir = project(":kotlin-gradle-plugin-integration-tests").projectDir
val generateNpmVersions by generator(
    "org.jetbrains.kotlin.generators.gradle.targets.js.MainKt",
    sourceSets["main"]
) {
    // to be compatible with Configuration cache to not catch top-level val into Task action
    val npmProjectDefault = npmProjectDefault
    outputs.dir(npmProjectDefault)
    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.targets.js.outputSourceRoot",
        kotlinGradlePluginProjectDir.resolve("src/common/kotlin").absolutePath
    )
    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.targets.js.npmPackageRoot",
        npmProjectDefault.get().asFile.absolutePath
    )
    systemProperty(
        "org.jetbrains.kotlin.npm.tooling.name",
        kotlinWebNpmToolingDirName
    )
}

val mainGradlePluginLocationForLockFiles =
    kotlinGradlePluginProjectDir
        .resolve("src/common/resources/org/jetbrains/kotlin/gradle/targets/js")
        .relativeTo(rootDir)

val allLocationsForPackageJsonFile = listOf(
    kotlinGradlePluginIntegrationTestsProjectDir
        .resolve("src/test/resources/testProject/kotlin-wasm-tooling-inside-project"),
).map { it.relativeTo(rootDir) }

// Lock files always should be near package.json files and some other locations
val allLocationsForLockFiles = allLocationsForPackageJsonFile +
        mainGradlePluginLocationForLockFiles

val npmProjectSetup = project.layout.buildDirectory.map { it.dir("npm-project-installed") }
val setupNpmProject by tasks.registering(Copy::class) {
    dependsOn(generateNpmVersions)
    from(npmProjectDefault)
    into(npmProjectSetup)
}

val npmInstallDeps by tasks.registering(NpmTask::class) {
    workingDir.set(npmProjectSetup.map { it.file(".") })
    dependsOn(setupNpmProject)
    args.set(listOf("install"))
}

val yarnProjectSetup = project.layout.buildDirectory.map { it.dir("yarn-project-installed") }
val setupYarnProject by tasks.registering(Copy::class) {
    dependsOn(generateNpmVersions)
    from(npmProjectDefault)
    into(yarnProjectSetup)
}

val yarnInstallDeps by tasks.registering(YarnTask::class) {
    workingDir.set(yarnProjectSetup)
    dependsOn(setupYarnProject)
    args.set(listOf("install"))
}

val setupNpmFiles by tasks.registering(Copy::class) {
    dependsOn(yarnInstallDeps)
    dependsOn(npmInstallDeps)
    into(rootDir)

    allLocationsForLockFiles.forEach {
        from(npmProjectSetup.map { it.file("package-lock.json") }) {
            into(it.resolve("npm"))
        }
    }
    allLocationsForLockFiles.forEach {
        from(yarnProjectSetup.map { it.file("yarn.lock") }) {
            into(it.resolve("yarn"))
        }
    }

    allLocationsForPackageJsonFile.forEach {
        from(npmProjectDefault) {
            into(it.resolve("npm"))
        }

        from(npmProjectDefault) {
            into(it.resolve("yarn"))
        }
    }
}

// The task is supposed to run manually to upgrade NPM versions and lock files
val generateAll by tasks.registering {
    dependsOn(
        generateNpmVersions,
        setupNpmFiles,
    )
}