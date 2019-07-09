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
    archives(project(":kotlin-test:kotlin-test-js"))
}

node {
    version = "11.9.0"
    download = true
    nodeModulesDir = projectDir
}

tasks {
    "yarn" {
        outputs.upToDateWhen {
            projectDir.resolve("node_modules").isDirectory
        }
    }

    create<YarnTask>("yarnBuild") {
        group = "build"

        dependsOn("yarn")
        setWorkingDir(projectDir)
        args = listOf("build")

        inputs.dir("src")
        inputs.files(
            "cli.ts",
            "nodejs-source-map-support.js",
            "package.json",
            "rollup.config.js",
            "tsconfig.json",
            "yarn.lock"
        )
        outputs.dir("lib")
    }

    create<Delete>("cleanYarn") {
        group = "build"

        delete = setOf(
            "node_modules",
            "lib",
            ".rpt2_cache"
        )
    }

    getByName("clean").dependsOn("cleanYarn")
}

val jar = tasks.create<Jar>("jar") {
    dependsOn("yarnBuild")
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