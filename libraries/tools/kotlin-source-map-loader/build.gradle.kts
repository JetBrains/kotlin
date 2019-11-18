import com.moowork.gradle.node.yarn.YarnTask

description = "Source Map Loader for Webpack"

plugins {
    id("base")
    id("com.moowork.node")
}

val default = configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
val archives = configurations.getByName(Dependency.ARCHIVES_CONFIGURATION)

default.extendsFrom(archives)

plugins.apply("maven")

convention.getPlugin(MavenPluginConvention::class.java).also {
    it.conf2ScopeMappings.addMapping(MavenPlugin.RUNTIME_PRIORITY, archives, Conf2ScopeMappingContainer.RUNTIME)
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

        inputs.files(
            "kotlin-source-map-loader.js",
            "package.json",
            "yarn.lock"
        )
        outputs.dir("lib")
    }

    register<YarnTask>("yarnTest") {
        group = "verification"

        dependsOn("yarn")
        setWorkingDir(projectDir)
        args = listOf("test")

        inputs.dir(
            "test"
        )
        inputs.files(
            "kotlin-source-map-loader.js",
            "package.json",
            "yarn.lock"
        )
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

    named("check") {
        dependsOn("yarnTest")
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