import org.jetbrains.kotlin.pill.PillExtension

description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

val compilerClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

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
    compilerClasspath(project(":kotlin-reflect"))
    compilerClasspath(kotlinStdlib())
    compilerClasspath(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    compilerClasspath(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    compilerClasspath(project(":kotlin-compiler-embeddable"))
    compilerClasspath(project(":kotlin-scripting-compiler-embeddable"))
    compilerClasspath(project(":kotlin-scripting-compiler-impl-embeddable"))
    compilerClasspath(project(":kotlin-script-runtime"))
    compilerClasspath(project(":kotlin-scripting-jvm"))
    compileOnly(project(":compiler:cli-common")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":core:util.runtime")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":daemon-common")) // TODO: fix import (workaround for jps build)
}

projectTest {
    dependsOn(compilerClasspath)
    val compilerClasspathProvider = project.provider { compilerClasspath.asPath }
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerClasspathProvider.get())
    }
}
