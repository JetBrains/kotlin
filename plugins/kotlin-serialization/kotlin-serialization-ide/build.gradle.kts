
description = "Kotlinx Serialization IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":js:js.translator"))

    compile(project(":kotlinx-serialization-compiler-plugin"))
    compile(project(":idea"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))

    testCompileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    testCompileOnly(intellijDep())
    testRuntimeOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    testRuntimeOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

projectTest(parallel = true) {

}