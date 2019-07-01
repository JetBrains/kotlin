
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-core"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(intellijDep())
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

