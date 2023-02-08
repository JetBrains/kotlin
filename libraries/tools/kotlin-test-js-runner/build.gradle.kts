import com.github.gradle.node.yarn.task.YarnTask

description = "Simple Kotlin/JS tests runner with TeamCity reporter"

plugins {
    id("base")
    id("com.github.node-gradle.node") version "3.2.1"
}

publish()

val default = configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
default.extendsFrom(configurations.publishedRuntime.get())

node {
    version.set(nodejsVersion)
    download.set(true)
    nodeProjectDir.set(projectDir)
}

tasks {
    named("yarn") {
        val nodeModulesDir = projectDir.resolve("node_modules")
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }
        // Without it several yarns can works incorrectly
        (this as YarnTask).apply {
            args.set(args.get() + "--network-concurrency" + "1" + "--mutex" + "network")
        }
    }

    register<YarnTask>("yarnBuild") {
        group = "build"

        dependsOn("yarn")
        workingDir.set(projectDir)
        args.set(listOf("build"))

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
            "detect-correct-browser.js",
            "package.json",
            "rollup.config.js",
            "tsconfig.json",
            "yarn.lock"
        )
        outputs.dir("lib")
    }

    register<Delete>("cleanYarn") {
        group = "build"

        delete = setOf(
            "node_modules",
            "lib",
            ".rpt2_cache"
        )
    }

    named("clean") {
        dependsOn("cleanYarn")
    }
}

val jar by tasks.creating(Jar::class) {
    dependsOn(tasks.named("yarnBuild"))
    from(projectDir.resolve("lib"))
    from(projectDir.resolve("package.json"))
}

artifacts {
    add(configurations.archives.name, jar)
    add(configurations.publishedRuntime.name, jar)
}
