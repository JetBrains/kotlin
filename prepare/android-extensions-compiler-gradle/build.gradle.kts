
import org.gradle.jvm.tasks.Jar

description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compileOnly(commonDep("com.google.android", "android"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    embedded(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()

