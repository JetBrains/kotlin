import com.github.gradle.node.npm.task.NpxTask

description = "Simple Kotlin/Wasm devtools formatters"

plugins {
    base
    id("share-kotlin-wasm-custom-formatters")
    alias(libs.plugins.gradle.node)
}

node {
    version.set(nodejsVersion)
    download.set(true)
    nodeProjectDir.set(projectDir)
    npmInstallCommand.set("ci")
}

dependencies {
    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")
}

val npmBuild by tasks.registering(NpxTask::class) {
    group = "build"

    dependsOn(tasks.npmInstall)

    command.set("rollup")
    workingDir.set(projectDir)
    args.set(listOf("-c", "rollup.config.mjs", "--silent"))
    environment.set(mapOf("NODE_OPTIONS" to "--disable-warning=ExperimentalWarning"))

    inputs.dir("src")
    inputs.file("rollup.config.mjs")
    outputs.file("build/out/custom-formatters.js")
}

tasks.npmInstall {
    val nodeModulesDir = projectDir.resolve("node_modules")
    outputs.upToDateWhen {
        nodeModulesDir.isDirectory
    }

    if (gradle.startParameter.isOffline) {
        args.add("--offline")
    }

    args.add("--ignore-scripts")
}

tasks.clean {
    delete(layout.projectDirectory.dir("node_modules"))
}

configurations.wasmCustomFormattersProvider.configure {
    outgoing {
        artifact(npmBuild)
    }
}
