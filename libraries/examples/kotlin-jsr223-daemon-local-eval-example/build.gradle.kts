
description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

plugins {
    kotlin("jvm")
}

val compilerClasspath by configurations.creating

dependencies {
    testCompile(kotlinStdlib())
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-script-util"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    compilerClasspath(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compilerClasspath(projectRuntimeJar(":kotlin-scripting-compiler-embeddable"))
    compilerClasspath(project(":kotlin-reflect"))
    compilerClasspath(kotlinStdlib())
    compilerClasspath(project(":kotlin-script-runtime"))
    compilerClasspath(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compileOnly(project(":compiler:cli-common")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":core:util.runtime")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":compiler:daemon-common")) // TODO: fix import (workaround for jps build)
}

projectTest {
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerClasspath.asFileTree.asPath)
    }
}
