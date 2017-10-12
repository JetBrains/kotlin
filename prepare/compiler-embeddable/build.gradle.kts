
description = "Kotlin Compiler (embeddable)"

plugins {
    `java`
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
}

noDefaultJar()

runtimeJar(embeddableCompiler())

sourcesJar()
javadocJar()

publish()

