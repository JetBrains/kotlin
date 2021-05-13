
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
    compile(project(":plugins:uast-kotlin-base"))
    compile(project(":plugins:uast-kotlin-fir"))
    compile(project(":plugins:uast-kotlin-idea-base"))
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

