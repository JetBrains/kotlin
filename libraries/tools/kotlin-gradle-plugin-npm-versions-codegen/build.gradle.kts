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
    version.set(nodejsVersion)
}

val npmProjectDefault = project.layout.buildDirectory.map { it.dir("npm-project") }
val kotlinGradlePluginProjectDir = project(":kotlin-gradle-plugin").projectDir
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
}

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

val setupPackageLock by tasks.registering(Copy::class) {
    dependsOn(npmInstallDeps)
    from(npmProjectSetup.map { it.file("package-lock.json") })
    into(kotlinGradlePluginProjectDir.resolve("src/common/resources/org/jetbrains/kotlin/gradle/targets/js/npm").absolutePath)
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

val setupYarnLock by tasks.registering(Copy::class) {
    dependsOn(yarnInstallDeps)
    from(yarnProjectSetup.map { it.file("yarn.lock") })
    into(kotlinGradlePluginProjectDir.resolve("src/common/resources/org/jetbrains/kotlin/gradle/targets/js/yarn").absolutePath)
}

val generateAll by tasks.registering {
    dependsOn(
        generateNpmVersions,
        setupPackageLock,
        setupYarnLock
    )
}