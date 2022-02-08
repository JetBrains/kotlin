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
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    compileOnly(commonDependency("com.google.android", "android"))
    compileOnly(intellijCore())

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

