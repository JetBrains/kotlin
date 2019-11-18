import com.moowork.gradle.node.yarn.YarnTask

description = "Simple Kotlin/JS tests runner with TeamCity reporter"

plugins {
    id("base")
    id("com.moowork.node") version "1.2.0"
}

val default = configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
val archives = configurations.getByName(Dependency.ARCHIVES_CONFIGURATION)

default.extendsFrom(archives)

plugins.apply("maven")

convention.getPlugin(MavenPluginConvention::class.java).also {
    it.conf2ScopeMappings.addMapping(MavenPlugin.RUNTIME_PRIORITY, archives, Conf2ScopeMappingContainer.RUNTIME)
}

dependencies {
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        archives(project(":kotlin-test:kotlin-test-js"))
    }
}

node {
    version = "11.9.0"
    download = true
    nodeModulesDir = projectDir
}

tasks {
    named("yarn") {
        outputs.upToDateWhen {
            projectDir.resolve("node_modules").isDirectory
        }
        // Without it several yarns can works incorrectly
        (this as YarnTask).apply {
            args = args + "--network-concurrency" + "1" + "--mutex" + "network"
        }
    }

    register<YarnTask>("yarnBuild") {
        group = "build"

        dependsOn("yarn")
        setWorkingDir(projectDir)
        args = listOf("build")

        inputs.dir("src")
        inputs.files(
            "nodejs.ts",
            "karma.ts",
            "karma-kotlin-reporter.js",
            "karma-debug-runner.js",
            "mocha-kotlin-reporter.js",
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
}

artifacts {
    add(
        "archives",
        jar.archiveFile.get().asFile
    ) {
        builtBy(jar)
    }
}

publish()