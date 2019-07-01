
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-gradle"))
    compileOnly(intellijDep())
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
