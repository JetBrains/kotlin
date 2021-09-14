
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
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

