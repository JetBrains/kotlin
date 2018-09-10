
description = "Kotlinx Serialization IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlinx-serialization-compiler-plugin"))
    compile(project(":idea"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
    compileOnly(intellijDep())
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

