import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

description = "Simple Kotlin/Wasm devtools formatters"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("base")
    id("share-kotlin-wasm-custom-formatters")
    alias(libs.plugins.gradle.node)
}

node {
    version.set(nodejsVersion)
    download.set(true)
    nodeProjectDir.set(projectDir)
    npmInstallCommand.set("ci")
    distBaseUrl.set(null as String?)
}

dependencies {
    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")
}

val cleanBuild = tasks.register("cleanBuild", Delete::class) {
    group = "build"

    delete = setOf("build")
}

val cleanNpm = tasks.register("cleanNpm", Delete::class) {
    group = "build"

    delete = setOf("node_modules")
}

val npmBuild = tasks.register("npmBuild", NpxTask::class) {
    group = "build"

    dependsOn(tasks.npmInstall)

    val rollupConfigMjsFile = file("rollup.config.mjs")
    inputs.file(rollupConfigMjsFile)
        .withPropertyName("rollupConfigMjsFile")
        .normalizeLineEndings()
        .withPathSensitivity(RELATIVE)

    inputs.dir("src")
        .withPropertyName("src")
        .normalizeLineEndings()
        .withPathSensitivity(RELATIVE)

    command.set("rollup")
    workingDir.set(projectDir)
    args.set(listOf("-c", rollupConfigMjsFile.name))
    environment.set(mapOf("NODE_OPTIONS" to "--disable-warning=ExperimentalWarning"))

    outputs.file("build/out/custom-formatters.js")
}

tasks {
    npmInstall {
        val nodeModulesDir = projectDir.resolve("node_modules")
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }

        if (gradle.startParameter.isOffline) {
            args.add("--offline")
        }

        args.add("--ignore-scripts")
    }

    clean {
        dependsOn(cleanNpm, cleanBuild)
    }
}

configurations.wasmCustomFormattersProvider.configure {
    outgoing {
        artifact(npmBuild)
    }
}
