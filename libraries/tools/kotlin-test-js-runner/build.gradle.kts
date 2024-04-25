import com.github.gradle.node.npm.task.NpxTask

description = "Simple Kotlin/JS tests runner with TeamCity reporter"

plugins {
    id("base")
    alias(libs.plugins.gradle.node)
}

publish(sbom = false)

val sbom by configurations.creating {
    isCanBeResolved = true
    extendsFrom(configurations.publishedRuntime.get())
}

configureSbom(
    gradleConfigurations = setOf(sbom.name)
)


val default = configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
default.extendsFrom(configurations.publishedRuntime.get())

node {
    version.set(nodejsVersion)
    download.set(true)
    nodeProjectDir.set(projectDir)
}

dependencies {
    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")
}

tasks {
    named("npmInstall") {
        val nodeModulesDir = projectDir.resolve("node_modules")
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }
    }

    register<NpxTask>("npmBuild") {
        group = "build"

        dependsOn("npmInstall", "cleanLib", "fromStaticToLib")

        command.set("rollup")
        workingDir.set(projectDir)
        args.set(listOf("-c", "rollup.config.mjs", "--silent"))

        inputs.dir("src")
        inputs.files(
            "nodejs.ts",
            "nodejs-empty.ts",
            "nodejs-idle.ts",
            "karma.ts",
            "karma-kotlin-reporter.js",
            "karma-kotlin-debug-plugin.js",
            "karma-debug-runner.js",
            "karma-debug-framework.js",
            "mocha-kotlin-reporter.js",
            "tc-log-appender.js",
            "tc-log-error-webpack.js",
            "webpack-5-debug.js",
            "package.json",
            "rollup.config.mjs",
            "tsconfig.json",
            "package-lock.json"
        )
        outputs.dir("lib")
    }

    register<Copy>("fromStaticToLib") {
        group = "build"

        dependsOn("cleanLib")

        from(projectDir.resolve("static"))
        into("lib/static")
    }

    register<Delete>("cleanLib") {
        group = "build"

        delete = setOf(
            "lib",
        )
    }

    register<Delete>("cleanNpm") {
        group = "build"

        dependsOn("cleanLib")

        delete = setOf(
            "node_modules",
        )
    }

    named("clean") {
        dependsOn("cleanNpm")
    }
}

val jar by tasks.creating(Jar::class) {
    dependsOn(tasks.named("npmBuild"))
    from(projectDir.resolve("lib"))
    from(projectDir.resolve("package.json"))
}

artifacts {
    add(configurations.archives.name, jar)
    add(configurations.publishedRuntime.name, jar)
}
