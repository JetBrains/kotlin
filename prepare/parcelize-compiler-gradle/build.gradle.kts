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
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    compileOnly(commonDependency("com.google.android", "android"))
    compileOnly(intellijCore())

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