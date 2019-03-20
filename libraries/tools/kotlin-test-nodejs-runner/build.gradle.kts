import com.moowork.gradle.node.yarn.YarnTask

plugins {
    id("base")
    id("com.moowork.node") version "1.2.0"
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

        inputs.dir(projectDir.resolve("src"))
        outputs.file(projectDir.resolve("lib/kotlin-test-nodejs-runner.js"))
    }

    create<Delete>("cleanYarn") {
        group = "build"

        delete = setOf(
            projectDir.resolve("node_modules"),
            projectDir.resolve("lib"),
            projectDir.resolve(".rpt2_cache")
        )
    }

    getByName("clean").dependsOn("cleanYarn")
}

artifacts {
    add("archives", projectDir.resolve("lib/kotlin-test-nodejs-runner.js")) {
        builtBy("yarnBuild")
    }
}