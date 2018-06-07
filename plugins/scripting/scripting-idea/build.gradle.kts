
description = "Kotlin Scripting IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-scripting-compiler"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-core"))
    compileOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

