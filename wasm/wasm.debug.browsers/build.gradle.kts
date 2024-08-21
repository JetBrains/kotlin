import com.github.gradle.node.npm.task.NpxTask

description = "Simple Kotlin/Wasm devtools formatters"

plugins {
    id("base")
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

val cleanBuild by tasks.registering(Delete::class) {
    group = "build"

    delete = setOf("build")
}

val cleanNpm by tasks.registering(Delete::class) {
    group = "build"

    dependsOn(cleanBuild)

    delete = setOf("node_modules")
}

val npmBuild by tasks.registering(NpxTask::class) {
    group = "build"

    dependsOn(tasks.npmInstall, cleanBuild)

    command.set("rollup")
    workingDir.set(projectDir)
    args.set(listOf("-c", "rollup.config.mjs", "--silent"))

    inputs.dir("src")
    outputs.file("build/out/custom-formatters.js")
}


tasks {
    npmInstall {
        val nodeModulesDir = projectDir.resolve("node_modules")
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }
        args.add("--ignore-scripts")
    }

    clean {
        dependsOn(cleanNpm)
    }
}

configurations.wasmCustomFormattersProvider.configure {
    outgoing {
        artifact(npmBuild)
    }
}
