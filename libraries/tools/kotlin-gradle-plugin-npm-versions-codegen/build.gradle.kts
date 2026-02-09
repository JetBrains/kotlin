import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.yarn.task.YarnTask
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.http)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    implementation("org.apache.velocity:velocity-engine-core:2.3")
    constraints {
        api(libs.apache.commons.lang)
    }
    implementation("org.slf4j:slf4j-simple:1.7.36")
}

node {
    download.set(true)
    version.set(nodejsVersion)
}

val npmProjectDefault: Provider<Directory> = project.layout.buildDirectory.dir("npm-project")
val npmProjectInstallationDir: Provider<Directory> = project.layout.buildDirectory.dir("npm-project-installed")
val yarnProjectInstallationDir: Provider<Directory> = project.layout.buildDirectory.dir("yarn-project-installed")

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

val setupNpmProject by tasks.registering(Sync::class) {
    from(generateNpmVersions) {
        include("package.json")
    }
    into(npmProjectInstallationDir)
}

val npmInstallDeps by tasks.registering(NpmTask::class) {
    workingDir.set(npmProjectInstallationDir.map { it.file(".") })
    dependsOn(setupNpmProject)
    args.set(listOf("install", "--package-lock-only"))
}

val setupYarnProject by tasks.registering(Sync::class) {
    val yarnProjectInstallationDir = yarnProjectInstallationDir

    from(generateNpmVersions) {
        include("package.json")
    }
    into(yarnProjectInstallationDir)

    val fs = serviceOf<FileSystemOperations>()
    doFirst {
        fs.delete { delete(yarnProjectInstallationDir) }
    }
}

val yarnInstallDeps by tasks.registering(YarnTask::class) {
    workingDir.set(yarnProjectInstallationDir)
    dependsOn(setupYarnProject)
    args.set(listOf("install", "--ignore-scripts"))
}

val setupNpmFiles by tasks.registering {
    dependsOn(yarnInstallDeps)
    dependsOn(npmInstallDeps)

    // to fix Configuration Cache problems
    val rootDir = rootDir
    val npmProjectDefault = npmProjectDefault

    inputs.dir(npmProjectDefault)

    val packageLockLocation = npmProjectInstallationDir.map { it.file("package-lock.json") }
    inputs.file(packageLockLocation)

    val yarnLockLocation = yarnProjectInstallationDir.map { it.file("yarn.lock") }
    inputs.file(yarnLockLocation)

    val allLocationsForNpmLockFiles = allLocationsForLockFiles.map { file: File ->
        file.resolve("npm").also {
            outputs.dir(it)
        }
    }

    val allLocationsForYarnLockFiles = allLocationsForLockFiles.map { file: File ->
        file.resolve("yarn").also {
            outputs.dir(it)
        }
    }

    val allLocationsForPackageJsonFile = allLocationsForPackageJsonFile.flatMap { file: File ->
        listOf(
            file.resolve("npm").also {
                outputs.dir(it)
            },
            file.resolve("yarn").also {
                outputs.dir(it)
            }
        )
    }

    val fs = serviceOf<FileSystemOperations>()

    doLast {
        fs.copy {
            into(rootDir)

            allLocationsForNpmLockFiles.forEach { file: File ->
                from(packageLockLocation) {
                    into(file)
                }
            }
            allLocationsForYarnLockFiles.forEach { file: File ->
                from(yarnLockLocation) {
                    into(file)
                }
            }

            allLocationsForPackageJsonFile.forEach { file: File ->
                from(npmProjectDefault) {
                    into(file)
                }
            }
        }
    }
}

val generateAll by tasks.registering {
    description = "Upgrade NPM versions and lock files." +
            "Should be run manually, see https://youtrack.jetbrains.com/articles/KT-A-530/Upgrade-NPM-dependencies-versions"
    dependsOn(
        generateNpmVersions,
        setupNpmFiles,
    )
}

// disable cache-redirector because it fails to resolve @swc/helpers
// (It replaces the slash `/` with `%2f` and tries to resolve https://abc.cloudfront.net/registry.npmjs.org/@swc%2fhelpers)
jsCacheRedirector.redirectNpmRegistry = false
