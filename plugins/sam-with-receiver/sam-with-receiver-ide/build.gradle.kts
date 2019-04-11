
description = "Kotlin SamWithReceiver IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:idea-core"))

    Ide.AS33.orHigher.not {
        compile(project(":idea:idea-android"))
    }

    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))

    compile(intellijDep()) { includeJars("openapi", "extensions", "util") }
    Platform[181].orHigher {
        compile(intellijDep()) { includeJars("platform-api") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
