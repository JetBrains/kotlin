
description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

plugins {
    kotlin("jvm")
}

val compilerClasspath by configurations.creating

dependencies {
    testCompile(project(":kotlin-stdlib"))
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-script-util"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    compilerClasspath(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compilerClasspath(project(":kotlin-reflect"))
    compilerClasspath(project(":kotlin-stdlib"))
    compilerClasspath(project(":kotlin-script-runtime"))
}

projectTest {
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerClasspath.asFileTree.asPath)
    }
}
