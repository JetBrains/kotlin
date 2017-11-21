import com.moowork.gradle.node.npm.NpmTask

plugins {
    id("com.moowork.node").version("1.2.0")
}

apply { plugin("base") }

description = "Run compiler tests on NodeJs"

node {
    download = true
}

tasks {
    "runMochaOnTeamCity"(NpmTask::class) {
        setArgs(listOf("run", "test"))
    }
    "runMocha"(NpmTask::class) {
        setArgs(listOf("run", "mocha"))
        dependsOn("npm_install")

        val check by tasks
        check.dependsOn(this)
    }
}