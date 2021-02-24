import org.gradle.jvm.tasks.Jar

description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":plugins:parcelize:parcelize-runtime"))
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compileOnly(commonDep("com.google.android", "android"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    embedded(project(":plugins:parcelize:parcelize-compiler")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()