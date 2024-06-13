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
    npmInstallCommand.set("ci")
}

dependencies {
    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")
}

tasks {
    npmInstall {
        val nodeModulesDir = projectDir.resolve("node_modules")
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }
        args.add("--ignore-scripts")
    }

    val cleanLib by registering(Delete::class) {
        group = "build"

        delete = setOf(
            "lib",
        )
    }

    val fromStaticToLib by registering(Copy::class) {
        group = "build"

        dependsOn(cleanLib)

        from(projectDir.resolve("static"))
        into("lib/static")
    }

    val cleanNpm by registering(Delete::class) {
        group = "build"

        dependsOn(cleanLib)

        delete = setOf(
            "node_modules",
        )
    }

    val test by registering(NpxTask::class) {
        group = "verification"

        val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

        dependsOn(npmInstall)

        command.set("mocha")

        if (isTeamcityBuild) {
            args.addAll(
                "--reporter",
                "mocha-teamcity-reporter"
            )
        }
    }

    val npmBuild by registering(NpxTask::class) {
        group = "build"

        dependsOn(npmInstall, cleanLib, fromStaticToLib)

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
            "karma-webpack-output.js",
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

    check {
        dependsOn(test)
    }

    clean {
        dependsOn(cleanNpm)
    }

    val jar by registering(Jar::class) {
        dependsOn(npmBuild)
        from(projectDir.resolve("lib"))
        from(projectDir.resolve("package.json"))
    }

    artifacts {
        add(configurations.archives.name, jar)
        add(configurations.publishedRuntime.name, jar)
    }
}
