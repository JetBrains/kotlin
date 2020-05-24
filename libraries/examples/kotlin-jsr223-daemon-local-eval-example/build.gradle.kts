import org.jetbrains.kotlin.pill.PillExtension

description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

val compilerClasspath by configurations.creating

dependencies {
    testCompile(kotlinStdlib())
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-script-util"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(projectRuntimeJar(":kotlin-daemon-embeddable"))
    testCompile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    compilerClasspath(project(":kotlin-compiler-embeddable"))
    compilerClasspath(project(":kotlin-scripting-compiler"))
    compilerClasspath(project(":kotlin-reflect"))
    compilerClasspath(kotlinStdlib())
    compilerClasspath(project(":kotlin-script-runtime"))
    compilerClasspath(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compileOnly(project(":compiler:cli-common")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":core:util.runtime")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":daemon-common")) // TODO: fix import (workaround for jps build)
}

projectTest {
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerClasspath.asFileTree.asPath)
    }
}
