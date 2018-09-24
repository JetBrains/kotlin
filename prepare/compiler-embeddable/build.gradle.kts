
description = "Kotlin Compiler (embeddable)"

plugins {
    `java`
}

dependencies {
    runtime(project(":kotlin-stdlib"))
    runtime(project(":kotlin-script-runtime"))
    runtime(project(":kotlin-reflect"))
}

noDefaultJar()

// dummy is used for rewriting dependencies to the shaded packages in the embeddable compiler
compilerDummyJar(compilerDummyForDependenciesRewriting("compilerDummy") {
    classifier = "dummy"
})

runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
}

sourcesJar()
javadocJar()

publish()

